package com.yuzu.backend.auth;

import java.time.Instant;
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

    public AuthService(RestauranteRepository restaurantes,
                       UsuarioRepository usuarios,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder,
                       @Value("${yuzu.jwt.minutos:480}") long minutos) {
        this.restaurantes = restaurantes;
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.minutos = minutos;
    }

    public LoginResponse login(LoginRequest request) {
        Restaurante restaurante = restaurantes
                .findBySubdominio(request.subdominio().trim().toLowerCase())
                .orElseThrow(AuthService::credencialesInvalidas);

        Usuario usuario = usuarios
                .findByRestauranteIdAndUsuario(restaurante.getId(), request.usuario().trim())
                .filter(Usuario::isActivo)
                .filter(u -> passwordEncoder.matches(request.clave(), u.getClaveHash()))
                .orElseThrow(AuthService::credencialesInvalidas);

        Instant ahora = Instant.now();
        Instant expira = ahora.plus(minutos, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("yuzu")
                .issuedAt(ahora)
                .expiresAt(expira)
                .subject(String.valueOf(usuario.getId()))
                .claim("restauranteId", restaurante.getId())
                .claim("usuario", usuario.getUsuario())
                .claim("rol", usuario.getRol())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new LoginResponse(token, minutos * 60,
                usuario.getUsuario(), usuario.getNombre(), usuario.getRol(), restaurante.getNombre());
    }

    private static ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o clave incorrectos");
    }
}