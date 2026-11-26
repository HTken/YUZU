package com.yuzu.backend.pago;

/**
 * El cargo necesita pasar por la autenticación 3D Secure (Culqi respondió HTTP 200 en vez de 201).
 * El frontend ya tiene todo lo que necesita (el tokenId) para iniciar la autenticación con Culqi3DS
 * y reintentar el cargo, así que no hace falta mandar más datos.
 */
final class Requiere3DSException extends RuntimeException {
}
