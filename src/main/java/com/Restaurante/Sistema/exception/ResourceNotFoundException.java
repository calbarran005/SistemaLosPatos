package com.Restaurante.Sistema.exception;

/**
 * Excepción personalizada que se lanza cuando un recurso solicitado
 * (mesa, pedido, cliente, ticket, empleado...) no existe en la base de datos.
 *
 * <p>Es manejada de forma centralizada por {@link GlobalExceptionHandler},
 * que la traduce en una respuesta HTTP 404 estructurada.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }

    /**
     * Atajo para construir un mensaje uniforme del tipo
     * "Mesa con id '5' no encontrada".
     */
    public ResourceNotFoundException(String recurso, String campo, Object valor) {
        super(String.format("%s con %s '%s' no encontrado(a)", recurso, campo, valor));
    }
}
