package com.yuzu.backend.config;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.yuzu.backend.plan.Plan;
import com.yuzu.backend.plan.PlanRepository;
import com.yuzu.backend.restaurante.Restaurante;
import com.yuzu.backend.restaurante.RestauranteRepository;
import com.yuzu.backend.usuario.Usuario;
import com.yuzu.backend.usuario.UsuarioRepository;

@Component
@Profile("local")
public class DatosDemo implements CommandLineRunner {

    private final RestauranteRepository restaurantes;
    private final UsuarioRepository usuarios;
    private final PlanRepository planes;

    public DatosDemo(RestauranteRepository restaurantes, UsuarioRepository usuarios, PlanRepository planes) {
        this.restaurantes = restaurantes;
        this.usuarios = usuarios;
        this.planes = planes;
    }

    @Override
    public void run(String... args) {
        if (restaurantes.findBySubdominio("demo").isPresent()) {
            return;
        }

        Plan plan = planes.findByCodigo("ESTANDAR").orElseThrow();

        Restaurante restaurante = new Restaurante();
        restaurante.setNombre("Restaurante Demo");
        restaurante.setSubdominio("demo");
        restaurante.setPlanId(plan.getId());
        restaurante.setVenceEn(LocalDate.now().plusDays(30));
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
