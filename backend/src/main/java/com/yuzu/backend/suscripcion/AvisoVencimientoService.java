package com.yuzu.backend.suscripcion;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.yuzu.backend.restaurante.Restaurante;
import com.yuzu.backend.usuario.Usuario;
import com.yuzu.backend.usuario.UsuarioRepository;

/**
 * Avisos por correo cuando el plan de un restaurante está por vencer, entró en gracia o fue suspendido.
 * Si el correo saliente no está configurado (en application-local.properties), solo se deja constancia
 * en el log: nunca debe romper la revisión de vencimientos.
 */
@Service
public class AvisoVencimientoService {

    private static final Logger log = LoggerFactory.getLogger(AvisoVencimientoService.class);

    private final UsuarioRepository usuarios;
    private final JavaMailSender mailSender;
    private final String remitente;

    public AvisoVencimientoService(UsuarioRepository usuarios,
                                   ObjectProvider<JavaMailSender> mailSenderProvider,
                                   @Value("${yuzu.correo.remitente:no-responder@yuzu.pe}") String remitente) {
        this.usuarios = usuarios;
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.remitente = remitente;
    }

    void avisarProximoAVencer(Restaurante restaurante, int diasRestantes) {
        enviarATodos(restaurante, ReglasAvisoVencimiento.proximoAVencer(
                restaurante.getNombre(), restaurante.getVenceEn().toString(), diasRestantes));
    }

    void avisarEnGracia(Restaurante restaurante, int diasGracia) {
        enviarATodos(restaurante, ReglasAvisoVencimiento.enGracia(
                restaurante.getNombre(), restaurante.getVenceEn().toString(), diasGracia));
    }

    void avisarSuspendido(Restaurante restaurante) {
        enviarATodos(restaurante, ReglasAvisoVencimiento.suspendido(restaurante.getNombre()));
    }

    private void enviarATodos(Restaurante restaurante, ReglasAvisoVencimiento.Correo correo) {
        List<String> destinatarios = usuarios
                .findByRestauranteIdAndRolAndActivoTrueAndEmailIsNotNull(restaurante.getId(), "ADMIN")
                .stream().map(Usuario::getEmail).toList();

        if (destinatarios.isEmpty()) {
            log.info("Restaurante {} no tiene un administrador con correo guardado: no se avisa ({})",
                    restaurante.getId(), correo.asunto());
            return;
        }
        if (mailSender == null) {
            log.warn("El correo saliente no está configurado: no se pudo avisar a {} ({})",
                    destinatarios, correo.asunto());
            return;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(destinatarios.toArray(new String[0]));
            mensaje.setSubject(correo.asunto());
            mensaje.setText(correo.cuerpo());
            mailSender.send(mensaje);
            log.info("Aviso de vencimiento enviado a {} ({})", destinatarios, correo.asunto());
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el aviso de vencimiento a {}: {}", destinatarios, e.toString());
        }
    }
}
