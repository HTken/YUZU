package com.yuzu.backend.plataforma;

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

/** El 2FA lo activa o desactiva cada administrador de plataforma para su propia cuenta. */
@Service
public class PlataformaDosFactoresService {

    private final AdministradorPlataformaRepository administradores;

    public PlataformaDosFactoresService(AdministradorPlataformaRepository administradores) {
        this.administradores = administradores;
    }

    public TotpEstadoDto estado() {
        return new TotpEstadoDto(actual().getTotpSecret() != null);
    }

    /** Genera un secreto nuevo; todavía no se guarda hasta que se confirme con {@link #activar}. */
    public TotpSetupDto iniciar() {
        AdministradorPlataforma admin = actual();
        String secreto = Totp.generarSecreto();
        return new TotpSetupDto(secreto, Totp.otpauthUrl(admin.getUsuario(), secreto));
    }

    public TotpEstadoDto activar(TotpActivarRequest request) {
        AdministradorPlataforma admin = actual();
        long ahora = Tiempo.ahora().toEpochSecond(ZoneOffset.UTC);
        if (!Totp.validar(request.secreto(), request.codigo().trim(), ahora)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El código no coincide. Revisa la hora de tu celular e inténtalo de nuevo.");
        }
        admin.setTotpSecret(request.secreto());
        administradores.save(admin);
        return new TotpEstadoDto(true);
    }

    public TotpEstadoDto desactivar() {
        AdministradorPlataforma admin = actual();
        admin.setTotpSecret(null);
        administradores.save(admin);
        return new TotpEstadoDto(false);
    }

    private AdministradorPlataforma actual() {
        return administradores.findById(SesionActual.usuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Administrador no encontrado"));
    }
}
