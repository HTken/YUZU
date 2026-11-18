package com.yuzu.backend.config;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.yuzu.backend.plan.Plan;
import com.yuzu.backend.plan.PlanRepository;
import com.yuzu.backend.plataforma.AdministradorPlataforma;
import com.yuzu.backend.plataforma.AdministradorPlataformaRepository;
import com.yuzu.backend.restaurante.Restaurante;
import com.yuzu.backend.restaurante.RestauranteRepository;
import com.yuzu.backend.usuario.Usuario;
import com.yuzu.backend.usuario.UsuarioRepository;

/** Datos de prueba. Solo se crean con el perfil "local", nunca en producción. */
@Component
@Profile("local")
public class DatosDemo implements CommandLineRunner {

    private final RestauranteRepository restaurantes;
    private final UsuarioRepository usuarios;
    private final PlanRepository planes;
    private final AdministradorPlataformaRepository administradores;

    public DatosDemo(RestauranteRepository restaurantes,
                     UsuarioRepository usuarios,
                     PlanRepository planes,
                     AdministradorPlataformaRepository administradores) {
        this.restaurantes = restaurantes;
        this.usuarios = usuarios;
        this.planes = planes;
        this.administradores = administradores;
    }

    @Override
    public void run(String... args) {
        crearRestauranteDemo();
        crearSuperadminDemo();
    }

    private void crearRestauranteDemo() {
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

    private void crearSuperadminDemo() {
        if (administradores.findByUsuario("superadmin").isPresent()) {
            return;
        }

        AdministradorPlataforma superadmin = new AdministradorPlataforma();
        superadmin.setUsuario("superadmin");
        superadmin.setNombre("Superadmin Demo");
        superadmin.setClaveHash(new BCryptPasswordEncoder().encode("superadmin123"));
        administradores.save(superadmin);
    }
}
