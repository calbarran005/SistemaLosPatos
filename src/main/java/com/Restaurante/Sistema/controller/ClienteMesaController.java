package com.Restaurante.Sistema.controller;

import com.Restaurante.Sistema.dto.PedidoRequest;
import com.Restaurante.Sistema.dto.ItemCarritoRequest;
import com.Restaurante.Sistema.entity.*;
import com.Restaurante.Sistema.repository.*;
import com.Restaurante.Sistema.service.MesaService;
import com.Restaurante.Sistema.service.PedidoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/mesa")
public class ClienteMesaController {

    private static final String SESS_CLIENTE_ID = "CLIENTE_ID";
    private static final String SESS_PEDIDO_ID = "PEDIDO_ACTIVO_ID";
    private static final String SESS_MESA_TOKEN = "MESA_TOKEN";

    private final MesaRepository mesaRepository;
    private final ClienteNaturalRepository clienteNaturalRepository;
    private final ClienteJuridicoRepository clienteJuridicoRepository;
    private final ClienteRepository clienteRepository;
    private final PlatoBebidaRepository platoBebidaRepository;
    private final CategoriaMenuRepository categoriaMenuRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoDetalleRepository detalleRepository;
    private final MesaService mesaService;
    private final PedidoService pedidoService;

    public ClienteMesaController(MesaRepository mesaRepository,
                                  ClienteNaturalRepository clienteNaturalRepository,
                                  ClienteJuridicoRepository clienteJuridicoRepository,
                                  ClienteRepository clienteRepository,
                                  PlatoBebidaRepository platoBebidaRepository,
                                  CategoriaMenuRepository categoriaMenuRepository,
                                  PedidoRepository pedidoRepository,
                                  PedidoDetalleRepository detalleRepository,
                                  MesaService mesaService,
                                  PedidoService pedidoService) {
        this.mesaRepository = mesaRepository;
        this.clienteNaturalRepository = clienteNaturalRepository;
        this.clienteJuridicoRepository = clienteJuridicoRepository;
        this.clienteRepository = clienteRepository;
        this.platoBebidaRepository = platoBebidaRepository;
        this.categoriaMenuRepository = categoriaMenuRepository;
        this.pedidoRepository = pedidoRepository;
        this.detalleRepository = detalleRepository;
        this.mesaService = mesaService;
        this.pedidoService = pedidoService;
    }

    @GetMapping("/{token}")
    public String landing(@PathVariable String token, HttpSession session) {
        Optional<Mesa> mesaOpt = mesaRepository.findByQrToken(token);
        if (mesaOpt.isEmpty()) return "redirect:/login";
        Mesa mesa = mesaOpt.get();
        session.setAttribute(SESS_MESA_TOKEN, token);

        Pedido pedidoActivo = findActivePedidoForMesa(mesa);
        
        // Si la mesa está libre, forzar identificación siempre (como estaba antes)
        if (pedidoActivo == null) {
            session.removeAttribute(SESS_CLIENTE_ID);
            session.removeAttribute(SESS_PEDIDO_ID);
            return "redirect:/mesa/" + token + "/auth";
        }

        // Si la mesa está ocupada, ver si el cliente en sesión es el mismo que el del pedido
        Integer sessClienteId = (Integer) session.getAttribute(SESS_CLIENTE_ID);
        if (sessClienteId != null && pedidoActivo.getCliente().getId_cliente().equals(sessClienteId)) {
            session.setAttribute(SESS_PEDIDO_ID, pedidoActivo.getId_pedido());
            return "redirect:/mesa/" + token + "/menu";
        }

        // En cualquier otro caso, ir a la pantalla de auth/identificación
        return "redirect:/mesa/" + token + "/auth";
    }

    @GetMapping("/{token}/auth")
    public String authForm(@PathVariable String token, Model model, HttpSession session) {
        Optional<Mesa> mesaOpt = mesaRepository.findByQrToken(token);
        if (mesaOpt.isEmpty()) return "redirect:/login";
        Mesa mesa = mesaOpt.get();
        Pedido pedidoActivo = findActivePedidoForMesa(mesa);
        
        if (pedidoActivo != null) {
            // Si yo soy el dueño del pedido, ir directo al menú
            Integer sessClienteId = (Integer) session.getAttribute(SESS_CLIENTE_ID);
            if (sessClienteId != null && pedidoActivo.getCliente().getId_cliente().equals(sessClienteId)) {
                return "redirect:/mesa/" + token + "/menu";
            }

            model.addAttribute("mesaOcupada", true);
            String nombreDisplay = "";
            if (pedidoActivo.getCliente() instanceof ClienteNatural) {
                ClienteNatural cn = (ClienteNatural) pedidoActivo.getCliente();
                nombreDisplay = cn.getNombre() + " " + cn.getApellidos();
            } else if (pedidoActivo.getCliente() instanceof ClienteJuridico) {
                ClienteJuridico cj = (ClienteJuridico) pedidoActivo.getCliente();
                nombreDisplay = cj.getRazonSocial();
            }
            model.addAttribute("clienteNombre", nombreDisplay);
        }

        model.addAttribute("mesa", mesa);
        model.addAttribute("token", token);
        return "Mesa/auth-cliente";
    }

    @GetMapping("/{token}/auth/buscar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscarCliente(
            @PathVariable String token,
            @RequestParam(required = false) String documento) {
        Optional<Mesa> mesaOpt = mesaRepository.findByQrToken(token);
        if (mesaOpt.isEmpty()) return ResponseEntity.notFound().build();
        Mesa mesa = mesaOpt.get();
        if (documento == null || documento.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of("encontrado", false));
        }
        String doc = documento.trim();
        Pedido pedidoActivo = findActivePedidoForMesa(mesa);
        if (pedidoActivo != null) {
            String docDuenio = "";
            if (pedidoActivo.getCliente() instanceof ClienteNatural) {
                docDuenio = ((ClienteNatural) pedidoActivo.getCliente()).getDni();
            } else if (pedidoActivo.getCliente() instanceof ClienteJuridico) {
                docDuenio = ((ClienteJuridico) pedidoActivo.getCliente()).getRuc();
            }
            if (!doc.equals(docDuenio)) {
                return ResponseEntity.ok(Map.of("encontrado", false, "error", "Esta mesa está ocupada por otro cliente."));
            }
        }
        if (doc.length() == 11) {
            return clienteJuridicoRepository.findByRuc(doc)
                    .map(c -> {
                        Map<String, Object> r = new HashMap<>();
                        r.put("encontrado", true);
                        r.put("tipo", "JURIDICO");
                        r.put("id", c.getId_cliente());
                        r.put("nombre", c.getRazonSocial());
                        r.put("ruc", c.getRuc());
                        r.put("representante", c.getRepresentante());
                        r.put("telefono", c.getTelefono());
                        r.put("email", c.getEmail());
                        return ResponseEntity.ok(r);
                    })
                    .orElse(ResponseEntity.ok(Map.of("encontrado", false, "tipo", "JURIDICO")));
        }
        return clienteNaturalRepository.findByDni(doc)
                .map(c -> {
                    Map<String, Object> r = new HashMap<>();
                    r.put("encontrado", true);
                    r.put("tipo", "NATURAL");
                    r.put("id", c.getId_cliente());
                    r.put("nombre", c.getNombre());
                    r.put("apellidos", c.getApellidos());
                    r.put("dni", c.getDni());
                    r.put("telefono", c.getTelefono());
                    r.put("email", c.getEmail());
                    return ResponseEntity.ok(r);
                })
                .orElse(ResponseEntity.ok(Map.of("encontrado", false, "tipo", "NATURAL")));
    }

    @PostMapping("/{token}/auth")
    public String procesarAuth(@PathVariable String token,
                                @RequestParam(required = false) Integer clienteId,
                                @RequestParam(required = false) String tipoRegistro,
                                @RequestParam(required = false) String nombre,
                                @RequestParam(required = false) String apellidos,
                                @RequestParam(required = false) String dni,
                                @RequestParam(required = false) String ruc,
                                @RequestParam(required = false) String razonSocial,
                                @RequestParam(required = false) String representante,
                                @RequestParam(required = false) String telefono,
                                @RequestParam(required = false) String email,
                                HttpSession session) {
        Optional<Mesa> mesaOpt = mesaRepository.findByQrToken(token);
        if (mesaOpt.isEmpty()) return "redirect:/login";
        Mesa mesa = mesaOpt.get();
        Integer idGuardado = null;
        if (clienteId != null) {
            idGuardado = clienteId;
        } else if ("JURIDICO".equals(tipoRegistro)) {
            ClienteJuridico cj = clienteJuridicoRepository.findByRuc(ruc.trim()).orElse(new ClienteJuridico());
            cj.setTipoCliente(Cliente.TipoCliente.JURIDICO);
            cj.setRuc(ruc.trim());
            cj.setRazonSocial(razonSocial != null ? razonSocial.trim() : "");
            cj.setRepresentante(representante);
            cj.setTelefono(telefono);
            cj.setEmail(email);
            idGuardado = clienteJuridicoRepository.save(cj).getId_cliente();
        } else {
            ClienteNatural cn = clienteNaturalRepository.findByDni(dni.trim()).orElse(new ClienteNatural());
            cn.setTipoCliente(Cliente.TipoCliente.NATURAL);
            cn.setNombre(nombre != null ? nombre.trim() : "");
            cn.setApellidos(apellidos != null ? apellidos.trim() : "");
            cn.setDni(dni.trim());
            cn.setTelefono(telefono);
            cn.setEmail(email);
            idGuardado = clienteNaturalRepository.save(cn).getId_cliente();
        }
        session.setAttribute(SESS_CLIENTE_ID, idGuardado);
        session.setAttribute(SESS_MESA_TOKEN, token);
        Pedido pedidoActivo = findActivePedidoForMesa(mesa);
        if (pedidoActivo != null) session.setAttribute(SESS_PEDIDO_ID, pedidoActivo.getId_pedido());
        else session.removeAttribute(SESS_PEDIDO_ID);
        return "redirect:/mesa/" + token + "/menu";
    }

    @GetMapping("/{token}/menu")
    public String menu(@PathVariable String token, Model model, HttpSession session) {
        Optional<Mesa> mesaOpt = mesaRepository.findByQrToken(token);
        if (mesaOpt.isEmpty()) return "redirect:/login";
        Mesa mesa = mesaOpt.get();
        
        Pedido pedidoActivo = findActivePedidoForMesa(mesa);
        Integer sessClienteId = (Integer) session.getAttribute(SESS_CLIENTE_ID);

        // Seguridad: Si no hay sesión o la mesa está ocupada por otro, redirigir a auth
        if (sessClienteId == null) {
            return "redirect:/mesa/" + token + "/auth";
        }
        if (pedidoActivo != null && !pedidoActivo.getCliente().getId_cliente().equals(sessClienteId)) {
            return "redirect:/mesa/" + token + "/auth";
        }

        if (pedidoActivo != null) session.setAttribute(SESS_PEDIDO_ID, pedidoActivo.getId_pedido());
        else session.removeAttribute(SESS_PEDIDO_ID);

        Cliente cliente = clienteRepository.findById(sessClienteId).orElse(null);
        List<Map<String, Object>> categoriasDto = new ArrayList<>();
        categoriaMenuRepository.findAll().forEach(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id_categoria", c.getId_categoria());
            m.put("nombre", c.getNombre());
            categoriasDto.add(m);
        });
        List<Map<String, Object>> itemsDto = new ArrayList<>();
        platoBebidaRepository.findAll().forEach(i -> {
            if (i.isDisponible()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id_item", i.getId_item());
                m.put("nombre", i.getNombre());
                m.put("descripcion", i.getDescripcion());
                m.put("precioVenta", i.getPrecioVenta());
                m.put("imagenUrl", i.getImagenUrl());
                if (i.getCategoria() != null) m.put("categoria", Map.of("id_categoria", i.getCategoria().getId_categoria()));
                itemsDto.add(m);
            }
        });
        model.addAttribute("mesa", mesa);
        model.addAttribute("token", token);
        model.addAttribute("cliente", cliente);
        model.addAttribute("categorias", categoriasDto);
        model.addAttribute("items", itemsDto);
        List<Map<String, Object>> detallesDto = new ArrayList<>();
        if (pedidoActivo != null) {
            model.addAttribute("pedidoActivo", pedidoActivo);
            detalleRepository.findByPedido(pedidoActivo).forEach(d -> {
                Map<String, Object> map = new HashMap<>();
                map.put("cantidad", d.getCantidad());
                map.put("subtotal", d.getSubtotal());
                map.put("platoBebida", Map.of(
                        "id_item", d.getPlatoBebida().getId_item(),
                        "nombre", d.getPlatoBebida().getNombre(),
                        "precioVenta", d.getPlatoBebida().getPrecioVenta(),
                        "imagenUrl", d.getPlatoBebida().getImagenUrl() != null ? d.getPlatoBebida().getImagenUrl() : ""
                ));
                detallesDto.add(map);
            });
        }
        model.addAttribute("detallesActivos", detallesDto);
        return "Mesa/menu-cliente";
    }

    @PostMapping("/{token}/pedido")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearPedido(@PathVariable String token,
                                                            @RequestBody PedidoRequest request,
                                                            HttpSession session) {
        Optional<Mesa> mesaOpt = mesaRepository.findByQrToken(token);
        if (mesaOpt.isEmpty()) return ResponseEntity.notFound().build();
        Mesa mesa = mesaOpt.get();
        Integer clienteId = (Integer) session.getAttribute(SESS_CLIENTE_ID);
        if (clienteId == null) return ResponseEntity.status(401).build();
        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El carrito está vacío"));
        }
        Pedido pedido = findActivePedidoForMesa(mesa);
        if (pedido == null) {
            pedido = pedidoService.crearPedido(mesa, cliente);
            mesaService.ocupar(mesa);
        } else if (!pedido.getCliente().getId_cliente().equals(clienteId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Mesa ocupada por otro cliente"));
        }
        pedidoService.sincronizarItems(pedido.getId_pedido(), request.getItems());
        session.setAttribute(SESS_PEDIDO_ID, pedido.getId_pedido());
        return ResponseEntity.ok(Map.of("pedidoId", pedido.getId_pedido(), "total", pedido.getTotal()));
    }

    @GetMapping("/{token}/estado")
    public String verEstado(@PathVariable String token, Model model, HttpSession session) {
        Optional<Mesa> mesaOpt = mesaRepository.findByQrToken(token);
        if (mesaOpt.isEmpty()) return "redirect:/login";
        Mesa mesa = mesaOpt.get();
        Pedido pedido = findActivePedidoForMesa(mesa);
        if (pedido == null) return "redirect:/mesa/" + token + "/menu";
        
        Integer sessClienteId = (Integer) session.getAttribute(SESS_CLIENTE_ID);
        if (sessClienteId == null || !pedido.getCliente().getId_cliente().equals(sessClienteId)) {
            return "redirect:/mesa/" + token + "/auth";
        }

        model.addAttribute("mesa", mesa);
        model.addAttribute("token", token);
        model.addAttribute("pedido", pedido);
        model.addAttribute("detalles", detalleRepository.findByPedido(pedido));
        return "Mesa/pedido-estado";
    }

    private Pedido findActivePedidoForMesa(Mesa mesa) {
        return pedidoRepository.findTopByMesaAndEstadoIn(mesa, List.of(
                Pedido.EstadoPedido.PENDIENTE,
                Pedido.EstadoPedido.PREPARANDO,
                Pedido.EstadoPedido.SERVIDO
        )).orElse(null);
    }
}
