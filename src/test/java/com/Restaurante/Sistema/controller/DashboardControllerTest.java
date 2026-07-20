package com.Restaurante.Sistema.controller;

import com.Restaurante.Sistema.config.MFAFilter;
import com.Restaurante.Sistema.repository.InsumoRepository;
import com.Restaurante.Sistema.repository.MesaRepository;
import com.Restaurante.Sistema.repository.PedidoDetalleRepository;
import com.Restaurante.Sistema.repository.PedidoRepository;
import com.Restaurante.Sistema.repository.PlatoBebidaRepository;
import com.Restaurante.Sistema.repository.TicketRepository;
import com.Restaurante.Sistema.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Prueba de integración del {@link DashboardController} con MockMvc.
 * Renderiza la vista real {@code Dashboard.html}, de modo que cualquier error
 * de expresión Thymeleaf haría fallar el test. Los repositorios se simulan con
 * Mockito (sin base de datos); las respuestas por defecto (listas vacías / 0)
 * ejercitan los "estados vacíos" del panel.
 */
@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private TicketRepository ticketRepository;
    @MockitoBean private PedidoRepository pedidoRepository;
    @MockitoBean private PedidoDetalleRepository detalleRepository;
    @MockitoBean private MesaRepository mesaRepository;
    @MockitoBean private InsumoRepository insumoRepository;
    @MockitoBean private PlatoBebidaRepository platoBebidaRepository;

    // Beans requeridos para construir la configuración de seguridad en el slice
    @MockitoBean private MFAFilter mfaFilter;
    @MockitoBean private UserRepository userRepository;

    @Test
    @DisplayName("GET /dashboard como ADMIN renderiza la vista con sus métricas")
    @WithMockUser(username = "admin@patos.com", roles = "ADMIN")
    void dashboard_renderizaParaAdmin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("Dashboard"))
                .andExpect(model().attributeExists(
                        "ventasSemana", "ventasHoy", "variacionSemanal",
                        "labels7dias", "ventas7dias",
                        "metodoLabels", "metodoValores",
                        "platoLabels", "platoValores",
                        "mesasOcupadas", "totalMesas",
                        "pedidosActivos", "platosDisponibles",
                        "pedidosRecientes", "insumosCriticos"));
    }
}
