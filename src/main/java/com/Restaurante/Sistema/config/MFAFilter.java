package com.Restaurante.Sistema.config;

import com.Restaurante.Sistema.entity.Usuario;
import com.Restaurante.Sistema.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class MFAFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public MFAFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();

        // Si el usuario está autenticado y no está en una página de login/mfa/recursos estáticos
        if (auth != null && auth.isAuthenticated() && 
            !path.equals("/login") && !path.equals("/mfa-verify") && !path.equals("/logout") &&
            !path.startsWith("/static/") && !path.endsWith(".css") && !path.endsWith(".jpg")) {

            userRepository.findByEmail(auth.getName()).ifPresent(usuario -> {
                // Si tiene MFA habilitado pero la sesión no está marcada como "mfa_verified"
                if (usuario.isMfaEnabled() && request.getSession().getAttribute("mfa_verified") == null) {
                    try {
                        response.sendRedirect("/mfa-verify");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            
            // Si el redirect ya se envió, detenemos la cadena
            if (response.isCommitted()) return;
        }

        filterChain.doFilter(request, response);
    }
}
