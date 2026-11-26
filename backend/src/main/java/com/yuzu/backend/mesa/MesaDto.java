package com.yuzu.backend.mesa;

public record MesaDto(
        Long id,
        Long salonId,
        String salon,
        String nombre,
        int capacidad,
        boolean activo) {

    public static MesaDto desde(Mesa m, String salon) {
        return new MesaDto(m.getId(), m.getSalonId(), salon, m.getNombre(), m.getCapacidad(), m.isActivo());
    }
}
