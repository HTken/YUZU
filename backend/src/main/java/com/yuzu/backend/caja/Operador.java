package com.yuzu.backend.caja;

import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.usuario.UsuarioRepository;

/** Nombre de quien hace una operación de caja, para dejar constancia. */
final class Operador {

    private Operador() {
    }

    static String nombre(UsuarioRepository usuarios, Long restauranteId) {
        return usuarios.findByIdAndRestauranteId(SesionActual.usuarioId(), restauranteId)
                .map(u -> u.getNombre())
                .orElseGet(SesionActual::usuario);
    }
}
