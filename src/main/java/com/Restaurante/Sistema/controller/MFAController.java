package com.Restaurante.Sistema.controller;

import com.Restaurante.Sistema.entity.Usuario;
import com.Restaurante.Sistema.repository.UserRepository;
import com.Restaurante.Sistema.service.MFAService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MFAController {

    private final MFAService mfaService;
    private final UserRepository userRepository;

    public MFAController(MFAService mfaService, UserRepository userRepository) {
        this.mfaService = mfaService;
        this.userRepository = userRepository;
    }

    @GetMapping("/mfa-verify")
    public String showMfaVerifyPage() {
        return "mfa-verify";
    }

    @PostMapping("/mfa-verify")
    public String verifyMfaCode(@RequestParam("code") String codeStr, HttpSession session, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "redirect:/login";

        try {
            int code = Integer.parseInt(codeStr);
            Usuario usuario = userRepository.findByEmail(auth.getName()).orElseThrow();

            if (mfaService.verifyCode(usuario.getMfaSecret(), code)) {
                session.setAttribute("mfa_verified", true);
                boolean isAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                return "redirect:" + (isAdmin ? "/dashboard" : "/pedidos");
            } else {
                model.addAttribute("error", "Código incorrecto. Inténtalo de nuevo.");
                return "mfa-verify";
            }
        } catch (NumberFormatException e) {
            model.addAttribute("error", "El código debe ser numérico.");
            return "mfa-verify";
        }
    }
}
