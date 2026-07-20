package com.Restaurante.Sistema.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;

/**
 * Manejo centralizado de excepciones para toda la aplicación.
 *
 * <p>Cada método anotado con {@link ExceptionHandler} captura un tipo de
 * excepción y construye una respuesta coherente. Se adapta al tipo de cliente:</p>
 * <ul>
 *   <li>Peticiones de API/AJAX (cabecera {@code Accept: application/json}) →
 *       reciben un {@link ErrorDetails} en JSON con el código HTTP adecuado.</li>
 *   <li>Peticiones de navegador (HTML) → se les muestra la vista {@code error.html}.</li>
 * </ul>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 404 — Recurso no encontrado (excepción de dominio personalizada).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        logger.warn("Recurso no encontrado en {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /**
     * 403 — Acceso denegado por rol insuficiente (lanzado por {@code @PreAuthorize}).
     * Sin este handler, el catch-all de abajo lo convertiría erróneamente en un 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        logger.warn("Acceso denegado en {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN,
                "No tiene permisos suficientes para acceder a este recurso.", request);
    }

    /**
     * 500 — Cualquier otra excepción no controlada explícitamente.
     */
    @ExceptionHandler(Exception.class)
    public Object handleGlobal(Exception ex, HttpServletRequest request) {
        logger.error("Error no controlado en {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error inesperado. Inténtelo de nuevo más tarde.", request);
    }

    /**
     * Construye la respuesta adecuada (JSON o vista HTML) según el cliente.
     */
    private Object build(HttpStatus status, String mensaje, HttpServletRequest request) {
        ErrorDetails detalles = new ErrorDetails(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                request.getRequestURI()
        );

        if (esPeticionJson(request)) {
            return ResponseEntity.status(status).body(detalles);
        }

        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("error", detalles);
        return mav;
    }

    /**
     * Determina si el cliente espera JSON: o bien la cabecera {@code Accept}
     * lo pide, o la URL corresponde a un endpoint de API.
     */
    private boolean esPeticionJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri.contains("/api") || uri.contains("/detalle") || uri.matches(".*/(pedido|estado|cerrar|servido)$");
    }
}
