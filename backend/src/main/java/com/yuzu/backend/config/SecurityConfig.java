package com.yuzu.backend.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecretKey clave;

    public SecurityConfig(@Value("${yuzu.jwt.secret}") String secreto) {
        this.clave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/plataforma/auth/login", "/error").permitAll()
                .requestMatchers("/api/plataforma/**").hasRole("SUPERADMIN")
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                .requestMatchers("/api/pagos/**").hasRole("ADMIN")
                // La carta la puede ver cualquier usuario del restaurante (los mozos la necesitan);
                // solo el administrador la modifica.
                .requestMatchers(HttpMethod.GET, "/api/categorias/**", "/api/productos/**").authenticated()
                .requestMatchers("/api/categorias/**", "/api/productos/**").hasRole("ADMIN")
                // Igual con los salones y mesas: todos los ven, solo el administrador los modifica.
                .requestMatchers(HttpMethod.GET, "/api/salones/**", "/api/mesas/**").authenticated()
                .requestMatchers("/api/salones/**", "/api/mesas/**").hasRole("ADMIN")
                // Pedidos: administrador, cajero y mozo los toman; anular algo ya enviado es solo del administrador.
                .requestMatchers("/api/pedidos/*/anular", "/api/pedidos/*/items/*/anular").hasRole("ADMIN")
                .requestMatchers("/api/pedidos/**").hasAnyRole("ADMIN", "CAJERO", "MOZO")
                // Cocina (ver lo que hay que preparar y marcarlo como listo): administrador y cocina.
                .requestMatchers("/api/cocina/**").hasAnyRole("ADMIN", "COCINA")
                // Caja (abrir, cerrar, cobrar, ingresos y egresos): administrador y cajero.
                // Anular una venta ya cobrada: solo el administrador (va antes de la regla de la caja).
                .requestMatchers("/api/caja/ventas/*/anular").hasRole("ADMIN")
                .requestMatchers("/api/caja/**").hasAnyRole("ADMIN", "CAJERO")
                // Inventario: administrador, cajero y cocina lo ven y registran entradas, salidas y mermas
                // (el ajuste por conteo lo limita el servicio al administrador). Crear o editar insumos: solo el administrador.
                // Las recetas (qué insumos gasta cada producto) las ve y edita solo el administrador.
                .requestMatchers("/api/inventario/recetas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/inventario/insumos/*/movimientos")
                    .hasAnyRole("ADMIN", "CAJERO", "COCINA")
                .requestMatchers(HttpMethod.GET, "/api/inventario/**").hasAnyRole("ADMIN", "CAJERO", "COCINA")
                .requestMatchers("/api/inventario/**").hasRole("ADMIN")
                // Informes (ventas, productos, insumos): solo el administrador.
                .requestMatchers("/api/informes/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorJwt())));
        return http.build();
    }

    /** Convierte el campo "rol" del token (por ejemplo ADMIN) en la autoridad ROLE_ADMIN. */
    private JwtAuthenticationConverter convertidorJwt() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("rol");
        autoridades.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
        return convertidor;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of(
                "http://localhost:4200", "http://*.localhost:4200", "http://localhost:4300"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
