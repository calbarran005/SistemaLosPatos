package com.Restaurante.Sistema.controller;

import com.Restaurante.Sistema.entity.Insumo;
import com.Restaurante.Sistema.entity.OrdenCompra;
import com.Restaurante.Sistema.entity.OrdenCompraDetalle;
import com.Restaurante.Sistema.entity.Proveedor;
import com.Restaurante.Sistema.repository.InsumoRepository;
import com.Restaurante.Sistema.repository.OrdenCompraRepository;
import com.Restaurante.Sistema.repository.ProveedorRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final ProveedorRepository proveedorRepo;
    private final InsumoRepository insumoRepo;
    private final OrdenCompraRepository ordenRepo;

    public ProveedorController(ProveedorRepository proveedorRepo,
                               InsumoRepository insumoRepo,
                               OrdenCompraRepository ordenRepo) {
        this.proveedorRepo = proveedorRepo;
        this.insumoRepo = insumoRepo;
        this.ordenRepo = ordenRepo;
    }

    @GetMapping
    public String pagina(Model model) {
        List<Insumo> insumos = insumoRepo.findAll();
        // Mapa id→unidad para que el front sepa cómo cobrar cada insumo (por kg, por L, por unidad)
        Map<Integer, String> insumosUnidad = new HashMap<>();
        insumos.forEach(i -> insumosUnidad.put(i.getId_insumo(), i.getUnidadMedida()));

        model.addAttribute("proveedores", proveedorRepo.findAll());
        model.addAttribute("insumos", insumos);
        model.addAttribute("insumosUnidad", insumosUnidad);
        model.addAttribute("ordenes", ordenRepo.findAllByOrderByFechaDesc());
        model.addAttribute("insumosCriticos", insumoRepo.findCriticos());
        return "Proveedores/proveedores";
    }

    /**
     * Factor de la unidad de compra respecto a la unidad de stock:
     * los insumos en gramos/mililitros se compran y cotizan por kilo/litro,
     * así que su costo unitario es "por 1000" de la unidad de stock.
     */
    private static double factorCompra(String unidad) {
        if (unidad == null) return 1.0;
        String u = unidad.trim().toUpperCase();
        return (u.equals("GRAMOS") || u.equals("MILILITROS")) ? 1000.0 : 1.0;
    }

    // ─────────────────────────────  PROVEEDORES  ─────────────────────────────

    @PostMapping("/guardar")
    public String guardarProveedor(@ModelAttribute Proveedor proveedor, RedirectAttributes ra) {
        String ruc = proveedor.getRuc() != null ? proveedor.getRuc().trim() : "";
        if (!ruc.matches("\\d{11}")) {
            ra.addFlashAttribute("error", "El RUC del proveedor debe tener exactamente 11 dígitos.");
            return "redirect:/proveedores";
        }
        boolean isNew = proveedor.getId_proveedor() == null;
        boolean rucDuplicado = isNew
                ? proveedorRepo.existsByRuc(ruc)
                : proveedorRepo.existsByRucExcluding(ruc, proveedor.getId_proveedor());
        if (rucDuplicado) {
            ra.addFlashAttribute("error", "Ya existe un proveedor con el RUC " + ruc + ".");
            return "redirect:/proveedores";
        }

        proveedor.setRuc(ruc);
        if (isNew) {
            proveedorRepo.save(proveedor);
            ra.addFlashAttribute("mensaje", "Proveedor \"" + proveedor.getRazonSocial() + "\" registrado.");
        } else {
            Proveedor existente = proveedorRepo.findById(proveedor.getId_proveedor()).orElseThrow();
            existente.setRazonSocial(proveedor.getRazonSocial());
            existente.setRuc(ruc);
            existente.setTelefono(proveedor.getTelefono());
            existente.setEmail(proveedor.getEmail());
            existente.setDireccion(proveedor.getDireccion());
            proveedorRepo.save(existente);
            ra.addFlashAttribute("mensaje", "Proveedor \"" + existente.getRazonSocial() + "\" actualizado.");
        }
        return "redirect:/proveedores";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarProveedor(@PathVariable Integer id, RedirectAttributes ra) {
        Proveedor p = proveedorRepo.findById(id).orElse(null);
        if (p == null) {
            ra.addFlashAttribute("error", "El proveedor no existe.");
            return "redirect:/proveedores";
        }
        try {
            proveedorRepo.delete(p);
            ra.addFlashAttribute("mensaje", "Proveedor \"" + p.getRazonSocial() + "\" eliminado.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se puede eliminar: el proveedor tiene órdenes de compra registradas.");
        }
        return "redirect:/proveedores";
    }

    // ───────────────────────────  INSUMOS (ALMACÉN)  ─────────────────────────

    @PostMapping("/insumos/guardar")
    public String guardarInsumo(@ModelAttribute Insumo insumo, RedirectAttributes ra) {
        if (insumo.getNombre() == null || insumo.getNombre().trim().isEmpty()) {
            ra.addFlashAttribute("error", "El nombre del insumo es obligatorio.");
            return "redirect:/proveedores";
        }
        if (insumo.getStockActual() == null) insumo.setStockActual(0.0);
        if (insumo.getStockMinimo() == null) insumo.setStockMinimo(0.0);
        if (insumo.getStockActual() < 0 || insumo.getStockMinimo() < 0) {
            ra.addFlashAttribute("error", "El stock no puede ser negativo.");
            return "redirect:/proveedores";
        }
        boolean isNew = insumo.getId_insumo() == null;
        if (isNew) {
            insumoRepo.save(insumo);
            ra.addFlashAttribute("mensaje", "Insumo \"" + insumo.getNombre() + "\" agregado al almacén.");
        } else {
            Insumo existente = insumoRepo.findById(insumo.getId_insumo()).orElseThrow();
            existente.setNombre(insumo.getNombre());
            existente.setUnidadMedida(insumo.getUnidadMedida());
            existente.setStockActual(insumo.getStockActual());
            existente.setStockMinimo(insumo.getStockMinimo());
            insumoRepo.save(existente);
            ra.addFlashAttribute("mensaje", "Insumo \"" + existente.getNombre() + "\" actualizado.");
        }
        return "redirect:/proveedores";
    }

    @GetMapping("/insumos/eliminar/{id}")
    public String eliminarInsumo(@PathVariable Integer id, RedirectAttributes ra) {
        Insumo i = insumoRepo.findById(id).orElse(null);
        if (i == null) {
            ra.addFlashAttribute("error", "El insumo no existe.");
            return "redirect:/proveedores";
        }
        try {
            insumoRepo.delete(i);
            ra.addFlashAttribute("mensaje", "Insumo \"" + i.getNombre() + "\" eliminado.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se puede eliminar: el insumo se usa en recetas u órdenes.");
        }
        return "redirect:/proveedores";
    }

    // ─────────────────────────  ÓRDENES DE COMPRA  ───────────────────────────

    @PostMapping("/ordenes/guardar")
    public String guardarOrden(@RequestParam Integer proveedorId,
                               @RequestParam(required = false) List<String> insumoId,
                               @RequestParam(required = false) List<String> cantidad,
                               @RequestParam(required = false) List<String> costo,
                               RedirectAttributes ra) {
        Proveedor proveedor = proveedorRepo.findById(proveedorId).orElse(null);
        if (proveedor == null) {
            ra.addFlashAttribute("error", "Selecciona un proveedor válido.");
            return "redirect:/proveedores";
        }
        if (insumoId == null || insumoId.isEmpty()) {
            ra.addFlashAttribute("error", "Agrega al menos un insumo a la orden de compra.");
            return "redirect:/proveedores";
        }

        OrdenCompra orden = new OrdenCompra();
        orden.setProveedor(proveedor);
        orden.setFecha(LocalDateTime.now());
        List<OrdenCompraDetalle> detalles = new ArrayList<>();
        double total = 0.0;

        for (int i = 0; i < insumoId.size(); i++) {
            Integer idIns = parseInt(insumoId.get(i));
            Double cant = parseDouble(cantidad != null && i < cantidad.size() ? cantidad.get(i) : null);
            Double cUnit = parseDouble(costo != null && i < costo.size() ? costo.get(i) : null);
            if (idIns == null || cant == null || cant <= 0) continue;   // salta filas vacías
            if (cUnit == null || cUnit < 0) cUnit = 0.0;

            Insumo insumo = insumoRepo.findById(idIns).orElse(null);
            if (insumo == null) continue;

            // El costo es por kilo/litro cuando el insumo se mide en gramos/mililitros
            double subtotal = (cant / factorCompra(insumo.getUnidadMedida())) * cUnit;

            OrdenCompraDetalle det = new OrdenCompraDetalle();
            det.setOrden(orden);
            det.setInsumo(insumo);
            det.setCantidad(cant);
            det.setCostoUnitario(cUnit);
            det.setSubtotal(subtotal);
            detalles.add(det);
            total += subtotal;

            // Ingresa la mercadería al almacén
            double stockActual = insumo.getStockActual() != null ? insumo.getStockActual() : 0.0;
            insumo.setStockActual(stockActual + cant);
            insumoRepo.save(insumo);
        }

        if (detalles.isEmpty()) {
            ra.addFlashAttribute("error", "Ingresa cantidades válidas (mayores a 0) para los insumos.");
            return "redirect:/proveedores";
        }

        orden.setDetalles(detalles);
        orden.setTotal(total);
        ordenRepo.save(orden);
        ra.addFlashAttribute("mensaje", "Orden de compra registrada. Stock actualizado en el almacén.");
        return "redirect:/proveedores";
    }

    @GetMapping("/ordenes/eliminar/{id}")
    public String eliminarOrden(@PathVariable Integer id, RedirectAttributes ra) {
        OrdenCompra o = ordenRepo.findById(id).orElse(null);
        if (o == null) {
            ra.addFlashAttribute("error", "La orden no existe.");
            return "redirect:/proveedores";
        }
        ordenRepo.delete(o);
        ra.addFlashAttribute("mensaje", "Orden de compra eliminada.");
        return "redirect:/proveedores";
    }

    private static Integer parseInt(String s) {
        try { return (s == null || s.isBlank()) ? null : Integer.valueOf(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private static Double parseDouble(String s) {
        try { return (s == null || s.isBlank()) ? null : Double.valueOf(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }
}
