package com.yuzu.backend.usuario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.suscripcion.SuscripcionService;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final SuscripcionService suscripcion;

    public UsuarioService(UsuarioRepository usuarios,
                          PasswordEncoder passwordEncoder,
                          SuscripcionService suscripcion) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.suscripcion = suscripcion;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        return usuarios.findByRestauranteIdOrderByNombreAsc(SesionActual.restauranteId())
                .stream()
                .map(UsuarioDto::desde)
                .toList();
    }

    @Transactional
    public UsuarioDto crear(UsuarioCrearRequest request) {
        // El restaurante sale del token, nunca de lo que envía el navegador.
        Long restauranteId = SesionActual.restauranteId();
        String login = request.usuario().trim().toLowerCase();

        if (usuarios.existsByRestauranteIdAndUsuario(restauranteId, login)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese usuario ya existe en tu restaurante");
        }
        suscripcion.exigirCupoDeUsuario(restauranteId);

        Usuario usuario = new Usuario();
        usuario.setRestauranteId(restauranteId);
        usuario.setUsuario(login);
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(limpiar(request.email()));
        usuario.setRol(validarRol(request.rol()));
        usuario.setClaveHash(passwordEncoder.encode(request.clave()));
        return UsuarioDto.desde(usuarios.save(usuario));
    }

    @Transactional
    public UsuarioDto actualizar(Long id, UsuarioActualizarRequest request) {
        Long restauranteId = SesionActual.restauranteId();

        // Si el usuario es de otro restaurante, se responde "no encontrado".
        Usuario usuario = usuarios.findByIdAndRestauranteId(id, restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String rol = validarRol(request.rol());
        boolean activo = request.activo();

        boolean esElMismo = usuario.getId().equals(SesionActual.usuarioId());
        if (esElMismo && (!activo || !Rol.ADMIN.name().equals(rol))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No puedes desactivarte ni quitarte el rol de administrador");
        }

        // Volver a activar a alguien también consume un cupo del plan.
        if (activo && !usuario.isActivo()) {
            suscripcion.exigirCupoDeUsuario(restauranteId);
        }

        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(limpiar(request.email()));
        usuario.setRol(rol);
        usuario.setActivo(activo);

        if (request.clave() != null && !request.clave().isBlank()) {
            usuario.setClaveHash(passwordEncoder.encode(request.clave()));
        }
        return UsuarioDto.desde(usuarios.save(usuario));
    }

    private static String validarRol(String rol) {
        try {
            return Rol.valueOf(rol.trim().toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol no es válido");
        }
    }

    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}
