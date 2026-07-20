package com.Restaurante.Sistema.controller;

import com.Restaurante.Sistema.exception.ErrorDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;

@Controller
public class MenuController {

    @GetMapping("/")
    public String index() {
        return "login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Página mostrada cuando un usuario autenticado intenta acceder a un recurso
     * para el que no tiene rol suficiente (HTTP 403). Reutiliza la vista de error
     * de la Sesión 08.
     */
    @GetMapping("/acceso-denegado")
    public String accesoDenegado(HttpServletRequest request, Model model) {
        model.addAttribute("error", new ErrorDetails(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "No tiene permisos suficientes para acceder a esta sección.",
                request.getRequestURI()
        ));
        return "error";
    }

}
