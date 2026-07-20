package com.Restaurante.Sistema.controller;

import com.Restaurante.Sistema.entity.Mesa;
import com.Restaurante.Sistema.entity.Pedido;
import com.Restaurante.Sistema.entity.Ticket;
import com.Restaurante.Sistema.repository.InsumoRepository;
import com.Restaurante.Sistema.repository.MesaRepository;
import com.Restaurante.Sistema.repository.PedidoDetalleRepository;
import com.Restaurante.Sistema.repository.PedidoRepository;
import com.Restaurante.Sistema.repository.PlatoBebidaRepository;
import com.Restaurante.Sistema.repository.TicketRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel de control con estadísticas reales del restaurante.
 * Solo accesible para administradores (también protegido por URL en SecurityConfig).
 */
@Controller
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final TicketRepository ticketRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoDetalleRepository detalleRepository;
    private final MesaRepository mesaRepository;
    private final InsumoRepository insumoRepository;
    private final PlatoBebidaRepository platoBebidaRepository;

    public DashboardController(TicketRepository ticketRepository,
                              PedidoRepository pedidoRepository,
                              PedidoDetalleRepository detalleRepository,
                              MesaRepository mesaRepository,
                              InsumoRepository insumoRepository,
                              PlatoBebidaRepository platoBebidaRepository) {
        this.ticketRepository = ticketRepository;
        this.pedidoRepository = pedidoRepository;
        this.detalleRepository = detalleRepository;
        this.mesaRepository = mesaRepository;
        this.insumoRepository = insumoRepository;
        this.platoBebidaRepository = platoBebidaRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime inicioSemana = hoy.minusDays(6).atStartOfDay();          // últimos 7 días
        LocalDateTime inicioSemanaPasada = hoy.minusDays(13).atStartOfDay();   // 7 días previos

        // ── Ventas (a partir de tickets emitidos) ──────────────────────────
        List<Ticket> ticketsRecientes = ticketRepository.findByFechaEmisionAfter(inicioSemanaPasada);

        double ventasHoy = 0, ventasSemana = 0, ventasSemanaPasada = 0;
        double[] serie = new double[7]; // venta por cada uno de los últimos 7 días

        for (Ticket t : ticketsRecientes) {
            if (t.getFechaEmision() == null || t.getTotal() == null) continue;
            LocalDateTime f = t.getFechaEmision();
            double val = t.getTotal();

            if (!f.isBefore(inicioSemana)) {
                ventasSemana += val;
                int idx = (int) ChronoUnit.DAYS.between(inicioSemana.toLocalDate(), f.toLocalDate());
                if (idx >= 0 && idx < 7) serie[idx] += val;
            } else if (!f.isBefore(inicioSemanaPasada)) {
                ventasSemanaPasada += val;
            }
            if (!f.isBefore(inicioHoy)) ventasHoy += val;
        }

        double variacion = ventasSemanaPasada > 0
                ? (ventasSemana - ventasSemanaPasada) / ventasSemanaPasada * 100.0
                : (ventasSemana > 0 ? 100.0 : 0.0);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        List<String> labels7 = new ArrayList<>();
        List<Double> ventas7 = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            labels7.add(hoy.minusDays(6 - i).format(fmt));
            ventas7.add(Math.round(serie[i] * 100.0) / 100.0);
        }

        model.addAttribute("ventasHoy", ventasHoy);
        model.addAttribute("ventasSemana", ventasSemana);
        model.addAttribute("variacionSemanal", variacion);
        model.addAttribute("labels7dias", labels7);
        model.addAttribute("ventas7dias", ventas7);

        // ── Ventas por método de pago (todos los tickets) ──────────────────
        List<String> metodoLabels = new ArrayList<>();
        List<Double> metodoValores = new ArrayList<>();
        for (Object[] r : ticketRepository.resumenPorMetodoPago()) {
            metodoLabels.add(r[0] != null ? r[0].toString() : "Sin método");
            metodoValores.add(((Number) r[2]).doubleValue());
        }
        model.addAttribute("metodoLabels", metodoLabels);
        model.addAttribute("metodoValores", metodoValores);

        // ── Top 5 platos más vendidos ──────────────────────────────────────
        List<String> platoLabels = new ArrayList<>();
        List<Long> platoValores = new ArrayList<>();
        for (Object[] r : detalleRepository.topPlatos(PageRequest.of(0, 5))) {
            platoLabels.add((String) r[0]);
            platoValores.add(((Number) r[1]).longValue());
        }
        model.addAttribute("platoLabels", platoLabels);
        model.addAttribute("platoValores", platoValores);
        model.addAttribute("platosEstrella", platoLabels.size());

        // ── Estado de las mesas ────────────────────────────────────────────
        long mesasOcupadas = mesaRepository.countByEstado(Mesa.EstadoMesa.OCUPADA);
        long mesasLibres = mesaRepository.countByEstado(Mesa.EstadoMesa.LIBRE);
        long mesasReservadas = mesaRepository.countByEstado(Mesa.EstadoMesa.RESERVADA);
        long mesasMantenimiento = mesaRepository.countByEstado(Mesa.EstadoMesa.MANTENIMIENTO);
        model.addAttribute("mesasOcupadas", mesasOcupadas);
        model.addAttribute("mesasLibres", mesasLibres);
        model.addAttribute("mesasReservadas", mesasReservadas);
        model.addAttribute("mesasMantenimiento", mesasMantenimiento);
        model.addAttribute("totalMesas", mesasOcupadas + mesasLibres + mesasReservadas + mesasMantenimiento);

        // ── Otros indicadores ──────────────────────────────────────────────
        long pedidosActivos = pedidoRepository.findByEstadoInOrderByFechaDesc(List.of(
                Pedido.EstadoPedido.PENDIENTE,
                Pedido.EstadoPedido.PREPARANDO,
                Pedido.EstadoPedido.SERVIDO)).size();
        model.addAttribute("pedidosActivos", pedidosActivos);
        model.addAttribute("platosDisponibles", platoBebidaRepository.countByDisponibleTrue());

        // ── Listados ───────────────────────────────────────────────────────
        model.addAttribute("pedidosRecientes", pedidoRepository.findTop8ByOrderByFechaDesc());
        model.addAttribute("insumosCriticos", insumoRepository.findCriticos());

        return "Dashboard";
    }
}
