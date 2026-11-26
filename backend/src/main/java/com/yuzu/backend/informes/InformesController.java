package com.yuzu.backend.informes;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los informes son solo del administrador. Ver SecurityConfig.
 * Las fechas llegan como AAAA-MM-DD (días de Lima, ambas incluidas). Se reciben como texto para poder
 * responder con un mensaje claro si falta alguna o está mal escrita.
 */
@RestController
@RequestMapping("/api/informes")
public class InformesController {

    private final InformesService informes;

    public InformesController(InformesService informes) {
        this.informes = informes;
    }

    @GetMapping("/ventas")
    public InformeVentasDto ventas(
            @RequestParam(required = false) String desde, @RequestParam(required = false) String hasta) {
        return informes.ventas(desde, hasta);
    }

    @GetMapping("/insumos")
    public InformeInsumosDto insumos(
            @RequestParam(required = false) String desde, @RequestParam(required = false) String hasta) {
        return informes.insumos(desde, hasta);
    }
}
