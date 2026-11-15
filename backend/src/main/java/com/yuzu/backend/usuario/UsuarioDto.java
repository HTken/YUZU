package com.yuzu.backend.usuario;

/** Lo que se envía al navegador. Nunca incluye la clave, ni siquiera cifrada. */
public record UsuarioDto(
        Long id,
        String usuario,
        String nombre,
        String email,
        String rol,
        boolean activo) {

    public static UsuarioDto desde(Usuario u) {
        return new UsuarioDto(u.getId(), u.getUsuario(), u.getNombre(), u.getEmail(), u.getRol(), u.isActivo());
    }
}
