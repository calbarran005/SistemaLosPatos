package com.Restaurante.Sistema.controller;

import com.Restaurante.Sistema.entity.Ticket;
import com.Restaurante.Sistema.exception.ResourceNotFoundException;
import com.Restaurante.Sistema.repository.PedidoDetalleRepository;
import com.Restaurante.Sistema.repository.TicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    private final TicketRepository ticketRepository;
    private final PedidoDetalleRepository detalleRepository;

    public TicketController(TicketRepository ticketRepository, PedidoDetalleRepository detalleRepository) {
        this.ticketRepository = ticketRepository;
        this.detalleRepository = detalleRepository;
    }

    private static final int TAMANO_PAGINA = 10;

    @GetMapping
    public String listar(
            @RequestParam(required = false) String metodo,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        // Normaliza filtros vacíos a null para desactivarlos en la consulta
        String metodoFiltro = (metodo != null && !metodo.isBlank()) ? metodo : null;
        String busqueda = (q != null && !q.isBlank()) ? q.trim() : null;

        Pageable pageable = PageRequest.of(Math.max(page, 0), TAMANO_PAGINA,
                Sort.by(Sort.Direction.DESC, "fechaEmision"));
        Page<Ticket> ticketsPage = ticketRepository.buscar(metodoFiltro, busqueda, pageable);

        model.addAttribute("ticketsPage", ticketsPage);
        model.addAttribute("tickets", ticketsPage.getContent());
        model.addAttribute("metodos", ticketRepository.findMetodosPago());
        model.addAttribute("metodoSel", metodoFiltro);
        model.addAttribute("q", busqueda);

        // Métricas globales (sobre todos los comprobantes, no solo la página)
        long totalTickets = ticketRepository.count();
        double ingresoTotal = ticketRepository.sumTotalIngresos();
        model.addAttribute("totalTickets", totalTickets);
        model.addAttribute("ingresoTotal", ingresoTotal);
        model.addAttribute("ticketPromedio", totalTickets > 0 ? ingresoTotal / totalTickets : 0.0);
        model.addAttribute("resumenMetodos", ticketRepository.resumenPorMetodoPago());

        return "Tickets/tickets";
    }

    @GetMapping("/{id}/imprimir")
    public String imprimir(@PathVariable Integer id, Model model) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));
        model.addAttribute("ticket", ticket);
        model.addAttribute("detalles", detalleRepository.findByPedido(ticket.getPedido()));
        return "Tickets/ticket-imprimir";
    }
}
