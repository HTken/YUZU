package com.yuzu.backend.plataforma;

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

import com.yuzu.backend.auth.ReglasIntentosLogin;
import com.yuzu.backend.auth.Totp;
import com.yuzu.backend.config.Tiempo;

@Service
public class PlataformaAuthService {

    private final AdministradorPlataformaRepository administradores;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long minutos;
    private final int maxIntentos;
    private final int minutosBloqueo;

    public PlataformaAuthService(AdministradorPlataformaRepository administradores,
                                 PasswordEncoder passwordEncoder,
                                 JwtEncoder jwtEncoder,
                                 @Value("${yuzu.jwt.minutos-plataforma:120}") long minutos,
                                 @Value("${yuzu.seguridad.max-intentos:5}") int maxIntentos,
                                 @Value("${yuzu.seguridad.minutos-bloqueo:15}") int minutosBloqueo) {
        this.administradores = administradores;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.minutos = minutos;
        this.maxIntentos = maxIntentos;
        this.minutosBloqueo = minutosBloqueo;
    }

    public PlataformaLoginResultado login(PlataformaLoginRequest request) {
        AdministradorPlataforma admin = administradores.findByUsuario(request.usuario().trim())
                .filter(AdministradorPlataforma::isActivo)
                .orElseThrow(PlataformaAuthService::credencialesInvalidas);

        LocalDateTime ahora = Tiempo.ahora();
        if (ReglasIntentosLogin.estaBloqueado(admin.getBloqueadoHasta(), ahora)) {
            throw bloqueado(ReglasIntentosLogin.minutosRestantes(admin.getBloqueadoHasta(), ahora));
        }

        if (!passwordEncoder.matches(request.clave(), admin.getClaveHash())) {
            throw registrarIntentoFallido(admin, ahora);
        }

        if (admin.getTotpSecret() != null) {
            String codigo = request.codigoTotp();
            if (codigo == null || codigo.isBlank()) {
                return PlataformaLoginResultado.requiere2FA();
            }
            if (!Totp.validar(admin.getTotpSecret(), codigo.trim(), ahora.toEpochSecond(ZoneOffset.UTC))) {
                throw registrarIntentoFallido(admin, ahora);
            }
        }

        admin.setIntentosFallidos(0);
        admin.setBloqueadoHasta(null);
        administradores.save(admin);

        Instant emitido = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("yuzu")
                .issuedAt(emitido)
                .expiresAt(emitido.plus(minutos, ChronoUnit.MINUTES))
                .subject(String.valueOf(admin.getId()))
                .claim("tipo", "PLATAFORMA")
                .claim("rol", "SUPERADMIN")
                .claim("usuario", admin.getUsuario())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return PlataformaLoginResultado.completo(
                new PlataformaLoginResponse(token, minutos * 60, admin.getUsuario(), admin.getNombre()));
    }

    private ResponseStatusException registrarIntentoFallido(AdministradorPlataforma admin, LocalDateTime ahora) {
        int intentos = ReglasIntentosLogin.siguienteIntento(admin.getIntentosFallidos());
        admin.setIntentosFallidos(intentos);
        if (ReglasIntentosLogin.debeBloquear(intentos, maxIntentos)) {
            admin.setBloqueadoHasta(ReglasIntentosLogin.calcularBloqueoHasta(ahora, minutosBloqueo));
            administradores.save(admin);
            return bloqueado(minutosBloqueo);
        }
        administradores.save(admin);
        return credencialesInvalidas();
    }

    private static ResponseStatusException bloqueado(long minutos) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Demasiados intentos fallidos. Intenta de nuevo en " + minutos
                        + (minutos == 1 ? " minuto." : " minutos."));
    }

    private static ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
    }
}
