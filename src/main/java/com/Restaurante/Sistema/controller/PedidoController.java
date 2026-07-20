package com.Restaurante.Sistema.controller;

import com.Restaurante.Sistema.entity.PedidoDetalle;
import com.Restaurante.Sistema.entity.Ticket;
import com.Restaurante.Sistema.service.PedidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public String pedidosPage(Model model) {
        return "Pedidos/pedidos";
    }

    @GetMapping(value = "/api", produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getPedidosJson() {
        List<Map<String, Object>> result = pedidoService.getPedidosActivos().stream().map(p -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id_pedido", p.getId_pedido());
            dto.put("estado", p.getEstado().name());
            dto.put("total", p.getTotal());
            dto.put("fecha", p.getFecha() != null ? p.getFecha().toString() : null);
            if (p.getMesa() != null) {
                dto.put("mesa", Map.of("numero", p.getMesa().getNumero(), "id_mesa", p.getMesa().getId_mesa()));
            }
            if (p.getCliente() != null) {
                dto.put("clienteId", p.getCliente().getId_cliente());
            }
            return dto;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/{id}/detalle", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDetalle(@PathVariable Integer id) {
        List<PedidoDetalle> detalles = pedidoService.getDetalles(id);
        Map<String, Object> result = new HashMap<>();
        result.put("detalles", detalles.stream().map(d -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", d.getId_detalle());
            item.put("nombre", d.getPlatoBebida().getNombre());
            item.put("cantidad", d.getCantidad());
            item.put("precio", d.getPlatoBebida().getPrecioVenta());
            item.put("subtotal", d.getSubtotal());
            item.put("servido", d.isServido());
            return item;
        }).toList());
        double total = detalles.stream().mapToDouble(PedidoDetalle::getSubtotal).sum();
        result.put("total", total);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/detalle/{detalleId}/servido")
    @ResponseBody
    public ResponseEntity<Void> marcarServido(
            @PathVariable Integer id,
            @PathVariable Integer detalleId,
            @RequestParam boolean servido) {
        pedidoService.marcarDetalleServido(detalleId, servido);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/estado")
    @ResponseBody
    public ResponseEntity<Void> cambiarEstado(@PathVariable Integer id, @RequestParam String estado) {
        pedidoService.actualizarEstado(id, estado);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cerrar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cerrarCuenta(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "Efectivo") String metodoPago) {
        Ticket ticket = pedidoService.cerrarCuenta(id, metodoPago);
        Map<String, Object> response = new HashMap<>();
        response.put("ticketId", ticket.getId_ticket());
        response.put("serie", ticket.getSerieNumero());
        response.put("total", ticket.getTotal());
        return ResponseEntity.ok(response);
    }
}
