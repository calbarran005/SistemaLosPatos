package com.Restaurante.Sistema.controller;

import com.Restaurante.Sistema.entity.Usuario;
import com.Restaurante.Sistema.exception.ResourceNotFoundException;
import com.Restaurante.Sistema.repository.RolRepository;
import com.Restaurante.Sistema.repository.UserRepository;
import com.Restaurante.Sistema.service.MFAService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/empleados")
public class EmpleadoController {

    private final UserRepository userRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final MFAService mfaService;

    public EmpleadoController(UserRepository userRepository, RolRepository rolRepository, 
                              PasswordEncoder passwordEncoder, MFAService mfaService) {
        this.userRepository = userRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.mfaService = mfaService;
    }

    @GetMapping
    public String listarEmpleados(Model model) {
        model.addAttribute("empleados", userRepository.findAll());
        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("nuevoUsuario", new Usuario());
        return "Empleados/empleados";
    }

    @PostMapping("/guardar")
    public String guardarEmpleado(@ModelAttribute Usuario usuario, RedirectAttributes redirectAttributes) {
        boolean isNew = usuario.getId_usuario() == null;
        
        if (isNew) {
            // Generar secreto MFA para nuevos usuarios
            String secret = mfaService.generateSecretKey();
            usuario.setMfaSecret(secret);
            usuario.setMfaEnabled(true);
            usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
            userRepository.save(usuario);
            
            String qrUrl = mfaService.getQRCodeURL(usuario.getEmail(), secret);
            redirectAttributes.addFlashAttribute("showMFA", true);
            redirectAttributes.addFlashAttribute("mfaSecret", secret);
            redirectAttributes.addFlashAttribute("mfaQrUrl", qrUrl);
            redirectAttributes.addFlashAttribute("mensaje", "Empleado registrado correctamente. Configure el MFA.");
        } else {
            Usuario existing = userRepository.findById(usuario.getId_usuario())
                    .orElseThrow(() -> new ResourceNotFoundException("Empleado", "id", usuario.getId_usuario()));
            existing.setNombre(usuario.getNombre());
            existing.setEmail(usuario.getEmail());
            existing.setRol(usuario.getRol());
            if (usuario.getContrasena() != null && !usuario.getContrasena().isEmpty()) {
                existing.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
            }
            userRepository.save(existing);
            redirectAttributes.addFlashAttribute("mensaje", "Empleado actualizado correctamente.");
        }
        
        return "redirect:/empleados";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/eliminar/{id}")
    public String eliminarEmpleado(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        userRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensaje", "Empleado eliminado correctamente.");
        return "redirect:/empleados";
    }

    @GetMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Usuario usuario = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", "id", id));
        usuario.setAccountNonLocked(!usuario.isAccountNonLocked());
        userRepository.save(usuario);
        String estado = usuario.isAccountNonLocked() ? "activada" : "desactivada";
        redirectAttributes.addFlashAttribute("mensaje", "Cuenta " + estado + " correctamente.");
        return "redirect:/empleados";
    }

    @GetMapping("/mfa/{id}")
    public String verMFA(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Usuario usuario = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", "id", id));
        if (usuario.getMfaSecret() == null || usuario.getMfaSecret().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El usuario no tiene MFA configurado.");
            return "redirect:/empleados";
        }
        
        String qrUrl = mfaService.getQRCodeURL(usuario.getEmail(), usuario.getMfaSecret());
        redirectAttributes.addFlashAttribute("showMFA", true);
        redirectAttributes.addFlashAttribute("mfaSecret", usuario.getMfaSecret());
        redirectAttributes.addFlashAttribute("mfaQrUrl", qrUrl);
        redirectAttributes.addFlashAttribute("mensaje", "Configuración MFA de " + usuario.getNombre());
        
        return "redirect:/empleados";
    }
}
