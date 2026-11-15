package com.yuzu.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.yuzu.backend.restaurante.Restaurante;
import com.yuzu.backend.restaurante.RestauranteRepository;
import com.yuzu.backend.usuario.Usuario;
import com.yuzu.backend.usuario.UsuarioRepository;

@Component
@Profile("local")
public class DatosDemo implements CommandLineRunner {

    private final RestauranteRepository restaurantes;
    private final UsuarioRepository usuarios;

    public DatosDemo(RestauranteRepository restaurantes, UsuarioRepository usuarios) {
        this.restaurantes = restaurantes;
        this.usuarios = usuarios;
    }

    @Override
    public void run(String... args) {
        if (restaurantes.findBySubdominio("demo").isPresent()) {
            return;
        }

        Restaurante restaurante = new Restaurante();
        restaurante.setNombre("Restaurante Demo");
        restaurante.setSubdominio("demo");
        restaurante = restaurantes.save(restaurante);

        Usuario admin = new Usuario();
        admin.setRestauranteId(restaurante.getId());
        admin.setUsuario("admin");
        admin.setNombre("Administrador Demo");
        admin.setRol("ADMIN");
        admin.setClaveHash(new BCryptPasswordEncoder().encode("admin123"));
        usuarios.save(admin);
    }
}