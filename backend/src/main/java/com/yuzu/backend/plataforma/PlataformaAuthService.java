package com.yuzu.backend.plataforma;

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

@Service
public class PlataformaAuthService {

    private final AdministradorPlataformaRepository administradores;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long minutos;

    public PlataformaAuthService(AdministradorPlataformaRepository administradores,
                                 PasswordEncoder passwordEncoder,
                                 JwtEncoder jwtEncoder,
                                 @Value("${yuzu.jwt.minutos-plataforma:120}") long minutos) {
        this.administradores = administradores;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.minutos = minutos;
    }

    public PlataformaLoginResponse login(PlataformaLoginRequest request) {
        AdministradorPlataforma admin = administradores.findByUsuario(request.usuario().trim())
                .filter(AdministradorPlataforma::isActivo)
                .filter(a -> passwordEncoder.matches(request.clave(), a.getClaveHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario o contraseña incorrectos"));

        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("yuzu")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(minutos, ChronoUnit.MINUTES))
                .subject(String.valueOf(admin.getId()))
                .claim("tipo", "PLATAFORMA")
                .claim("rol", "SUPERADMIN")
                .claim("usuario", admin.getUsuario())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new PlataformaLoginResponse(token, "Bearer", minutos * 60, admin.getUsuario(), admin.getNombre());
    }
}
