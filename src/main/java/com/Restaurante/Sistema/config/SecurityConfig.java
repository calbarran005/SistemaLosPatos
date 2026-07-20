package com.Restaurante.Sistema.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // Habilita @PreAuthorize / @PostAuthorize a nivel de método
public class SecurityConfig {

    private final MFAFilter mfaFilter;

    public SecurityConfig(MFAFilter mfaFilter) {
        this.mfaFilter = mfaFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/mesa/**", "/api/**", "/pedidos/**", "/mesas/**", "/tickets/**"))
            .authorizeHttpRequests(auth -> auth
                // Recursos públicos: login, estáticos y portal del cliente (QR de mesa)
                .requestMatchers("/login", "/style.css", "/logopatos.jpg", "/input.css", "/mfa-verify").permitAll()
                .requestMatchers("/mesa/**", "/ws/**").permitAll()
                .requestMatchers("/acceso-denegado").permitAll()
                // Autorización por rol (los roles se guardan como ROLE_ADMIN, ROLE_CAJERO, ...)
                .requestMatchers("/dashboard", "/empleados/**", "/proveedores/**").hasRole("ADMIN")
                .requestMatchers("/tickets/**").hasAnyRole("ADMIN", "CAJERO")
                // El resto (carta, pedidos, mesas, clientes, dashboard) requiere estar autenticado
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/acceso-denegado"))
            .addFilterAfter(mfaFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(authenticationSuccessHandler())
                .failureUrl("/login?error=true")
                .failureHandler(authenticationFailureHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .invalidSessionUrl("/login?expired=true")
                .maximumSessions(1)
                .expiredUrl("/login?expired=true")
                .maxSessionsPreventsLogin(false)
            );

        return http.build();
    }

    // 🔥 BEAN NUEVO: Para manejar eventos de sesión y respetar el timeout
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /**
     * Tras un login exitoso, redirige según el rol: los administradores van al
     * panel de control; el resto del personal a la pantalla de pedidos
     * (evita un 403 al aterrizar en /dashboard, que es solo para ADMIN).
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            response.sendRedirect(isAdmin ? "/dashboard" : "/pedidos");
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            String errorMessage = "error";
            
            if (exception instanceof LockedException) {
                errorMessage = "locked";
            } else if (exception instanceof DisabledException) {
                errorMessage = "disabled";
            } else if (exception instanceof BadCredentialsException) {
                errorMessage = "credentials";
            }
            
            response.sendRedirect("/login?error=" + errorMessage);
        };
    }
}