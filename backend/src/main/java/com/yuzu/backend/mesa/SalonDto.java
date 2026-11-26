package com.yuzu.backend.mesa;

public record SalonDto(
        Long id,
        String nombre,
        int orden,
        boolean activo,
        int mesas,
        int mesasActivas) {

    public static SalonDto desde(Salon s, int mesas, int mesasActivas) {
        return new SalonDto(s.getId(), s.getNombre(), s.getOrden(), s.isActivo(), mesas, mesasActivas);
    }
}
