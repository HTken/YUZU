package com.yuzu.backend.pago;

/** Lo que devuelve la librería Culqi3DS en el navegador, tal cual, para reintentar el cargo. */
public record Autenticacion3DSRequest(
        String eci,
        String xid,
        String cavv,
        String protocolVersion,
        String directoryServerTransactionId) {
}
