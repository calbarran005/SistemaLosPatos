package com.Restaurante.Sistema.service;

import com.Restaurante.Sistema.dto.PedidoNotificacion;
import com.Restaurante.Sistema.dto.ItemCarritoRequest;
import com.Restaurante.Sistema.entity.*;
import com.Restaurante.Sistema.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoDetalleRepository detalleRepository;
    private final PlatoBebidaRepository platoBebidaRepository;
    private final MesaRepository mesaRepository;
    private final TicketRepository ticketRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final InventarioService inventarioService;

    public PedidoService(PedidoRepository pedidoRepository,
                         PedidoDetalleRepository detalleRepository,
                         PlatoBebidaRepository platoBebidaRepository,
                         MesaRepository mesaRepository,
                         TicketRepository ticketRepository,
                         SimpMessagingTemplate messagingTemplate,
                         InventarioService inventarioService) {
        this.pedidoRepository = pedidoRepository;
        this.detalleRepository = detalleRepository;
        this.platoBebidaRepository = platoBebidaRepository;
        this.mesaRepository = mesaRepository;
        this.ticketRepository = ticketRepository;
        this.messagingTemplate = messagingTemplate;
        this.inventarioService = inventarioService;
    }

    public Pedido crearPedido(Mesa mesa, Cliente cliente) {
        Pedido pedido = new Pedido();
        pedido.setMesa(mesa);
        pedido.setCliente(cliente);
        pedido.setEstado(Pedido.EstadoPedido.PENDIENTE);
        pedido.setTotal(0.0);
        pedido.setFecha(LocalDateTime.now());
        return pedidoRepository.save(pedido);
    }

    public PedidoDetalle agregarItem(Integer pedidoId, Integer itemId, Integer cantidad) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        PlatoBebida plato = platoBebidaRepository.findById(itemId).orElseThrow();

        List<PedidoDetalle> detalles = detalleRepository.findByPedido(pedido);
        PedidoDetalle existente = detalles.stream()
                .filter(d -> d.getPlatoBebida().getId_item().equals(itemId))
                .findFirst().orElse(null);

        PedidoDetalle detalle;
        if (existente != null) {
            existente.setCantidad(existente.getCantidad() + cantidad);
            existente.setSubtotal(existente.getPlatoBebida().getPrecioVenta() * existente.getCantidad());

            // Si por error había más de uno, consolida borrando los duplicados
            Integer idPrincipal = existente.getId_detalle();
            detalles.stream()
                    .filter(d -> d.getPlatoBebida().getId_item().equals(itemId) && !d.getId_detalle().equals(idPrincipal))
                    .forEach(detalleRepository::delete);
            detalle = existente;
        } else {
            detalle = new PedidoDetalle();
            detalle.setPedido(pedido);
            detalle.setPlatoBebida(plato);
            detalle.setCantidad(cantidad);
            detalle.setSubtotal(plato.getPrecioVenta() * cantidad);
        }

        detalleRepository.save(detalle);
        // Descuenta del almacén los insumos de la cantidad recién agregada
        inventarioService.descontarPorVenta(plato, cantidad);
        recalcularTotal(pedido);
        return detalle;
    }

    public void sincronizarItems(Integer pedidoId, List<ItemCarritoRequest> items) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        List<PedidoDetalle> actuales = detalleRepository.findByPedido(pedido);

        // Cantidades que ya estaban en el pedido, por item (para calcular el delta)
        Map<Integer, Integer> cantidadesPrevias = actuales.stream()
                .collect(Collectors.toMap(
                        d -> d.getPlatoBebida().getId_item(),
                        PedidoDetalle::getCantidad,
                        Integer::sum
                ));

        // 1. Consolidar items del request por ID
        Map<Integer, Integer> consolidado = items.stream()
                .filter(i -> i.getItemId() != null && i.getCantidad() != null && i.getCantidad() > 0)
                .collect(Collectors.toMap(
                        ItemCarritoRequest::getItemId,
                        ItemCarritoRequest::getCantidad,
                        Integer::sum
                ));

        // 2. Actualizar o crear detalles
        for (Map.Entry<Integer, Integer> entry : consolidado.entrySet()) {
            Integer itemId = entry.getKey();
            Integer cantidad = entry.getValue();

            List<PedidoDetalle> detallesItem = actuales.stream()
                    .filter(d -> d.getPlatoBebida().getId_item().equals(itemId))
                    .collect(Collectors.toList());

            if (!detallesItem.isEmpty()) {
                PedidoDetalle principal = detallesItem.get(0);
                principal.setCantidad(cantidad);
                principal.setSubtotal(principal.getPlatoBebida().getPrecioVenta() * cantidad);
                detalleRepository.save(principal);

                // Borrar duplicados si los hay
                for (int i = 1; i < detallesItem.size(); i++) {
                    detalleRepository.delete(detallesItem.get(i));
                }
            } else {
                // Crear nuevo
                PlatoBebida plato = platoBebidaRepository.findById(itemId).orElseThrow();
                PedidoDetalle nuevo = new PedidoDetalle();
                nuevo.setPedido(pedido);
                nuevo.setPlatoBebida(plato);
                nuevo.setCantidad(cantidad);
                nuevo.setSubtotal(plato.getPrecioVenta() * cantidad);
                detalleRepository.save(nuevo);
            }

            // Descuenta del almacén solo el incremento pedido (delta positivo)
            int delta = cantidad - cantidadesPrevias.getOrDefault(itemId, 0);
            if (delta > 0) {
                PlatoBebida plato = platoBebidaRepository.findById(itemId).orElseThrow();
                inventarioService.descontarPorVenta(plato, delta);
            }
        }

        // 3. (Opcional) Podríamos borrar items que NO están en el carrito, 
        // pero por seguridad en restaurantes solemos mantener lo ya pedido.
        // Si se desea permitir borrar, se añadiría aquí.

        recalcularTotal(pedido);
        notificarActualizacion(pedido);
    }

    public void recalcularTotal(Pedido pedido) {
        List<PedidoDetalle> detalles = detalleRepository.findByPedido(pedido);
        double total = detalles.stream().mapToDouble(PedidoDetalle::getSubtotal).sum();
        pedido.setTotal(total);
        pedidoRepository.save(pedido);
    }

    public List<Pedido> getPedidosActivos() {
        return pedidoRepository.findByEstadoInOrderByFechaDesc(
            List.of(Pedido.EstadoPedido.PENDIENTE, Pedido.EstadoPedido.PREPARANDO, Pedido.EstadoPedido.SERVIDO)
        );
    }

    public List<PedidoDetalle> getDetalles(Integer pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        return detalleRepository.findByPedido(pedido);
    }

    public Pedido actualizarEstado(Integer pedidoId, String estadoStr) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        Pedido.EstadoPedido estado = Pedido.EstadoPedido.valueOf(estadoStr);
        pedido.setEstado(estado);
        pedido = pedidoRepository.save(pedido);

        PedidoNotificacion notif = buildNotificacion(pedido, "ESTADO_CAMBIADO");
        messagingTemplate.convertAndSend("/topic/pedidos", notif);
        return pedido;
    }

    public Ticket cerrarCuenta(Integer pedidoId, String metodoPago) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        pedido.setEstado(Pedido.EstadoPedido.PAGADO);
        pedidoRepository.save(pedido);

        Mesa mesa = pedido.getMesa();
        if (mesa != null) {
            mesa.setEstado(Mesa.EstadoMesa.LIBRE);
            mesaRepository.save(mesa);
        }

        Ticket ticket = new Ticket();
        ticket.setPedido(pedido);
        ticket.setTotal(pedido.getTotal());
        ticket.setFechaEmision(LocalDateTime.now());
        ticket.setMetodoPago(metodoPago);
        ticket.setSerieNumero(generarSerie());
        ticket = ticketRepository.save(ticket);

        PedidoNotificacion notif = buildNotificacion(pedido, "PAGADO");
        messagingTemplate.convertAndSend("/topic/pedidos", notif);

        return ticket;
    }

    public void marcarDetalleServido(Integer detalleId, boolean servido) {
        PedidoDetalle detalle = detalleRepository.findById(detalleId).orElseThrow();
        detalle.setServido(servido);
        detalleRepository.save(detalle);
        notificarActualizacion(detalle.getPedido());
    }

    public void notificarNuevoPedido(Pedido pedido) {
        messagingTemplate.convertAndSend("/topic/pedidos", buildNotificacion(pedido, "NUEVO"));
    }

    public void notificarActualizacion(Pedido pedido) {
        messagingTemplate.convertAndSend("/topic/pedidos", buildNotificacion(pedido, "ACTUALIZADO"));
    }

    private PedidoNotificacion buildNotificacion(Pedido pedido, String tipo) {
        String clienteNombre = "";
        if (pedido.getCliente() instanceof ClienteNatural cn) {
            clienteNombre = cn.getNombre() + " " + cn.getApellidos();
        } else if (pedido.getCliente() instanceof ClienteJuridico cj) {
            clienteNombre = cj.getRazonSocial();
        }

        List<PedidoDetalle> detalles = detalleRepository.findByPedido(pedido);
        int cantItems = detalles.stream().mapToInt(PedidoDetalle::getCantidad).sum();

        return new PedidoNotificacion(
            pedido.getId_pedido(),
            pedido.getMesa() != null ? pedido.getMesa().getNumero() : 0,
            clienteNombre,
            cantItems,
            pedido.getTotal(),
            tipo,
            pedido.getEstado().name()
        );
    }

    private String generarSerie() {
        int siguiente = ticketRepository.findUltimoTicket()
            .map(t -> t.getId_ticket() + 1)
            .orElse(1);
        return String.format("T-%04d", siguiente);
    }
}
