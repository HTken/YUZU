package com.yuzu.backend.negocio;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.config.Tiempo;
import com.yuzu.backend.restaurante.Restaurante;
import com.yuzu.backend.restaurante.RestauranteRepository;

/**
 * Datos del negocio para el ticket. El restaurante sale siempre del token. Ver y editar es solo del
 * administrador (ver SecurityConfig); el ticket los lee a través de datos().
 */
@Service
public class NegocioService {

    private final DatosNegocioRepository datos;
    private final RestauranteRepository restaurantes;

    public NegocioService(DatosNegocioRepository datos, RestauranteRepository restaurantes) {
        this.datos = datos;
        this.restaurantes = restaurantes;
    }

    @Transactional(readOnly = true)
    public NegocioDto obtener() {
        return datos(SesionActual.restauranteId());
    }

    /**
     * Los datos de un restaurante. Si nunca se guardaron, devuelve los de siempre: sin RUC ni dirección,
     * con el agradecimiento por defecto y papel de 80 mm.
     */
    @Transactional(readOnly = true)
    public NegocioDto datos(Long restauranteId) {
        String nombre = restaurantes.findById(restauranteId).map(Restaurante::getNombre).orElse("");
        return datos.findById(restauranteId)
                .map(d -> new NegocioDto(nombre, d.getRazonSocial(), d.getRuc(), d.getDireccion(), d.getTelefono(),
                        d.getMensajeTicket(), d.getAnchoTicketMm()))
                .orElseGet(() -> new NegocioDto(nombre, null, null, null, null,
                        ReglasNegocio.MENSAJE_POR_DEFECTO, ReglasNegocio.ANCHO_POR_DEFECTO));
    }

    @Transactional
    public NegocioDto guardar(NegocioRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        restaurantes.findById(restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante no encontrado"));

        // Se valida todo antes de tocar nada.
        String razonSocial = ReglasNegocio.texto(request.razonSocial(), ReglasNegocio.RAZON_MAXIMO, "La razón social");
        String ruc = ReglasNegocio.ruc(request.ruc());
        String direccion = ReglasNegocio.texto(request.direccion(), ReglasNegocio.DIRECCION_MAXIMO, "La dirección");
        String telefono = ReglasNegocio.telefono(request.telefono());
        String mensaje = ReglasNegocio.texto(request.mensajeTicket(), ReglasNegocio.MENSAJE_MAXIMO, "El mensaje");
        int ancho = ReglasNegocio.ancho(request.anchoTicketMm());

        DatosNegocio fila = datos.findById(restauranteId).orElseGet(() -> {
            DatosNegocio nueva = new DatosNegocio();
            nueva.setRestauranteId(restauranteId);
            return nueva;
        });
        fila.setRazonSocial(razonSocial);
        fila.setRuc(ruc);
        fila.setDireccion(direccion);
        fila.setTelefono(telefono);
        fila.setMensajeTicket(mensaje);
        fila.setAnchoTicketMm(ancho);
        fila.setActualizadoEn(Tiempo.ahora());
        datos.save(fila);

        return datos(restauranteId);
    }
}
