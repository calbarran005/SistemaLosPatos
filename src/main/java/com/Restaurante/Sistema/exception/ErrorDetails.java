package com.Restaurante.Sistema.exception;

import java.time.LocalDateTime;

/**
 * Estructura estándar de una respuesta de error de la API.
 *
 * <p>Se devuelve dentro de un {@code ResponseEntity} para los endpoints JSON,
 * de modo que el cliente reciba siempre el mismo formato:</p>
 *
 * <pre>
 * {
 *   "timestamp": "2026-06-19T12:30:45",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Mesa con id '5' no encontrado(a)",
 *   "path": "/pedidos/5/detalle"
 * }
 * </pre>
 */
public record ErrorDetails(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
