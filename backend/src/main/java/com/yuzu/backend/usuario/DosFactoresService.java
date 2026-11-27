package com.yuzu.backend.usuario;

import java.time.ZoneOffset;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.auth.Totp;
import com.yuzu.backend.auth.TotpActivarRequest;
import com.yuzu.backend.auth.TotpEstadoDto;
import com.yuzu.backend.auth.TotpSetupDto;
import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.config.Tiempo;

/** El 2FA lo activa o desactiva cada administrador para su propia cuenta. */
@Service
public class DosFactoresService {

    private final UsuarioRepository usuarios;

    public DosFactoresService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public TotpEstadoDto estado() {
        return new TotpEstadoDto(usuarioActual().getTotpSecret() != null);
    }

    /** Genera un secreto nuevo; todavía no se guarda hasta que se confirme con {@link #activar}. */
    public TotpSetupDto iniciar() {
        Usuario usuario = usuarioActual();
        String secreto = Totp.generarSecreto();
        return new TotpSetupDto(secreto, Totp.otpauthUrl(usuario.getUsuario(), secreto));
    }

    public TotpEstadoDto activar(TotpActivarRequest request) {
        Usuario usuario = usuarioActual();
        long ahora = Tiempo.ahora().toEpochSecond(ZoneOffset.UTC);
        if (!Totp.validar(request.secreto(), request.codigo().trim(), ahora)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El código no coincide. Revisa la hora de tu celular e inténtalo de nuevo.");
        }
        usuario.setTotpSecret(request.secreto());
        usuarios.save(usuario);
        return new TotpEstadoDto(true);
    }

    public TotpEstadoDto desactivar() {
        Usuario usuario = usuarioActual();
        usuario.setTotpSecret(null);
        usuarios.save(usuario);
        return new TotpEstadoDto(false);
    }

    private Usuario usuarioActual() {
        return usuarios.findByIdAndRestauranteId(SesionActual.usuarioId(), SesionActual.restauranteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}
