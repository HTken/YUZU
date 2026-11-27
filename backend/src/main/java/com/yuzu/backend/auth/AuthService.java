package com.yuzu.backend.auth;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.config.Tiempo;
import com.yuzu.backend.restaurante.Restaurante;
import com.yuzu.backend.restaurante.RestauranteRepository;
import com.yuzu.backend.usuario.Usuario;
import com.yuzu.backend.usuario.UsuarioRepository;

@Service
public class AuthService {

    private final RestauranteRepository restaurantes;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long minutos;
    private final int maxIntentos;
    private final int minutosBloqueo;

    public AuthService(RestauranteRepository restaurantes,
                       UsuarioRepository usuarios,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder,
                       @Value("${yuzu.jwt.minutos:480}") long minutos,
                       @Value("${yuzu.seguridad.max-intentos:5}") int maxIntentos,
                       @Value("${yuzu.seguridad.minutos-bloqueo:15}") int minutosBloqueo) {
        this.restaurantes = restaurantes;
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.minutos = minutos;
        this.maxIntentos = maxIntentos;
        this.minutosBloqueo = minutosBloqueo;
    }

    /**
     * Si el usuario tiene el 2FA activado y no mandó el código todavía, devuelve "falta el código" sin
     * tocar la cookie; el frontend vuelve a mandar esta misma petición con {@code codigoTotp} relleno.
     */
    public LoginResultado login(LoginRequest request) {
        Restaurante restaurante = restaurantes
                .findBySubdominio(request.subdominio().trim().toLowerCase())
                .orElseThrow(AuthService::credencialesInvalidas);

        Usuario usuario = usuarios
                .findByRestauranteIdAndUsuario(restaurante.getId(), request.usuario().trim())
                .filter(Usuario::isActivo)
                .orElseThrow(AuthService::credencialesInvalidas);

        LocalDateTime ahora = Tiempo.ahora();
        if (ReglasIntentosLogin.estaBloqueado(usuario.getBloqueadoHasta(), ahora)) {
            throw bloqueado(ReglasIntentosLogin.minutosRestantes(usuario.getBloqueadoHasta(), ahora));
        }

        if (!passwordEncoder.matches(request.clave(), usuario.getClaveHash())) {
            throw registrarIntentoFallido(usuario, ahora);
        }

        if (requiere2FA(usuario)) {
            String codigo = request.codigoTotp();
            if (codigo == null || codigo.isBlank()) {
                return LoginResultado.requiere2FA();
            }
            if (!Totp.validar(usuario.getTotpSecret(), codigo.trim(), ahora.toEpochSecond(ZoneOffset.UTC))) {
                throw registrarIntentoFallido(usuario, ahora);
            }
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarios.save(usuario);

        Instant emitido = Instant.now();
        Instant expira = emitido.plus(minutos, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("yuzu")
                .issuedAt(emitido)
                .expiresAt(expira)
                .subject(String.valueOf(usuario.getId()))
                .claim("restauranteId", restaurante.getId())
                .claim("usuario", usuario.getUsuario())
                .claim("rol", usuario.getRol())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return LoginResultado.completo(new LoginResponse(token, minutos * 60,
                usuario.getUsuario(), usuario.getNombre(), usuario.getRol(), restaurante.getNombre()));
    }

    /** El 2FA es opcional y, por ahora, solo para administradores (ver CLAUDE.md). */
    private boolean requiere2FA(Usuario usuario) {
        return "ADMIN".equals(usuario.getRol()) && usuario.getTotpSecret() != null;
    }

    private ResponseStatusException registrarIntentoFallido(Usuario usuario, LocalDateTime ahora) {
        int intentos = ReglasIntentosLogin.siguienteIntento(usuario.getIntentosFallidos());
        usuario.setIntentosFallidos(intentos);
        if (ReglasIntentosLogin.debeBloquear(intentos, maxIntentos)) {
            usuario.setBloqueadoHasta(ReglasIntentosLogin.calcularBloqueoHasta(ahora, minutosBloqueo));
            usuarios.save(usuario);
            return bloqueado(minutosBloqueo);
        }
        usuarios.save(usuario);
        return credencialesInvalidas();
    }

    private static ResponseStatusException bloqueado(long minutos) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Demasiados intentos fallidos. Intenta de nuevo en " + minutos
                        + (minutos == 1 ? " minuto." : " minutos."));
    }

    private static ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o clave incorrectos");
    }
}
