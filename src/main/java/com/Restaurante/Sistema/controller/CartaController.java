package com.Restaurante.Sistema.controller;

import com.Restaurante.Sistema.entity.CategoriaMenu;
import com.Restaurante.Sistema.entity.Insumo;
import com.Restaurante.Sistema.entity.PlatoBebida;
import com.Restaurante.Sistema.entity.PlatoInsumo;
import com.Restaurante.Sistema.repository.CategoriaMenuRepository;
import com.Restaurante.Sistema.repository.InsumoRepository;
import com.Restaurante.Sistema.repository.PlatoBebidaRepository;
import com.Restaurante.Sistema.repository.PlatoInsumoRepository;
import com.Restaurante.Sistema.service.InventarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/carta")
public class CartaController {

    private final PlatoBebidaRepository platoRepo;
    private final CategoriaMenuRepository categoriaRepo;
    private final PlatoInsumoRepository recetaRepo;
    private final InsumoRepository insumoRepo;
    private final InventarioService inventarioService;

    public CartaController(PlatoBebidaRepository platoRepo,
                          CategoriaMenuRepository categoriaRepo,
                          PlatoInsumoRepository recetaRepo,
                          InsumoRepository insumoRepo,
                          InventarioService inventarioService) {
        this.platoRepo = platoRepo;
        this.categoriaRepo = categoriaRepo;
        this.recetaRepo = recetaRepo;
        this.insumoRepo = insumoRepo;
        this.inventarioService = inventarioService;
    }

    @GetMapping
    public String listar(Model model) {
        List<PlatoBebida> items = platoRepo.findAll();
        long disponibles = items.stream().filter(PlatoBebida::isDisponible).count();

        // Porciones que se pueden preparar según el stock de insumos (-1 = sin receta)
        Map<Integer, Integer> porciones = new HashMap<>();
        for (PlatoBebida item : items) {
            porciones.put(item.getId_item(), inventarioService.porcionesDisponibles(item));
        }

        model.addAttribute("items", items);
        model.addAttribute("categorias", categoriaRepo.findAll());
        model.addAttribute("insumos", insumoRepo.findAll());
        model.addAttribute("disponiblesCount", disponibles);
        model.addAttribute("porciones", porciones);
        return "Carta/carta";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute PlatoBebida item,
                          @RequestParam("categoriaId") Integer categoriaId,
                          RedirectAttributes ra) {
        CategoriaMenu categoria = categoriaRepo.findById(categoriaId).orElseThrow();
        boolean isNew = item.getId_item() == null;

        if (isNew) {
            item.setCategoria(categoria);
            platoRepo.save(item);
            ra.addFlashAttribute("mensaje", "\"" + item.getNombre() + "\" agregado a la carta.");
        } else {
            PlatoBebida existing = platoRepo.findById(item.getId_item()).orElseThrow();
            existing.setNombre(item.getNombre());
            existing.setDescripcion(item.getDescripcion());
            existing.setPrecioVenta(item.getPrecioVenta());
            existing.setCategoria(categoria);
            existing.setDisponible(item.isDisponible());
            existing.setImagenUrl(item.getImagenUrl());
            platoRepo.save(existing);
            ra.addFlashAttribute("mensaje", "\"" + existing.getNombre() + "\" actualizado correctamente.");
        }
        return "redirect:/carta";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {
        PlatoBebida item = platoRepo.findById(id).orElseThrow();
        String nombre = item.getNombre();
        recetaRepo.deleteAll(recetaRepo.findByPlatoId(id));   // borra su receta primero
        platoRepo.deleteById(id);
        ra.addFlashAttribute("mensaje", "\"" + nombre + "\" eliminado de la carta.");
        return "redirect:/carta";
    }

    @GetMapping("/disponible/{id}")
    public String toggleDisponible(@PathVariable Integer id, RedirectAttributes ra) {
        PlatoBebida item = platoRepo.findById(id).orElseThrow();
        item.setDisponible(!item.isDisponible());
        platoRepo.save(item);
        String estado = item.isDisponible() ? "disponible" : "no disponible";
        ra.addFlashAttribute("mensaje", "\"" + item.getNombre() + "\" marcado como " + estado + ".");
        return "redirect:/carta";
    }

    // ─────────────────────────────  RECETA  ──────────────────────────────────

    /** Devuelve la receta (ingredientes) del plato + porciones disponibles. */
    @GetMapping(value = "/{id}/receta", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> receta(@PathVariable Integer id) {
        PlatoBebida plato = platoRepo.findById(id).orElse(null);
        if (plato == null) return ResponseEntity.notFound().build();

        List<Map<String, Object>> lineas = recetaRepo.findByPlatoId(id).stream().map(pi -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", pi.getId());
            m.put("insumoNombre", pi.getInsumo().getNombre());
            m.put("unidad", pi.getInsumo().getUnidadMedida());
            m.put("cantidad", pi.getCantidad());
            m.put("stock", pi.getInsumo().getStockActual());
            return m;
        }).toList();

        Map<String, Object> res = new HashMap<>();
        res.put("plato", plato.getNombre());
        res.put("lineas", lineas);
        res.put("porciones", inventarioService.porcionesDisponibles(plato));
        return ResponseEntity.ok(res);
    }

    /** Agrega un insumo a la receta del plato. */
    @PostMapping("/{id}/receta")
    public String agregarInsumoReceta(@PathVariable Integer id,
                                      @RequestParam Integer insumoId,
                                      @RequestParam Double cantidad,
                                      RedirectAttributes ra) {
        PlatoBebida plato = platoRepo.findById(id).orElse(null);
        Insumo insumo = insumoRepo.findById(insumoId).orElse(null);
        if (plato == null || insumo == null) {
            ra.addFlashAttribute("error", "Plato o insumo no válido.");
            return "redirect:/carta";
        }
        if (cantidad == null || cantidad <= 0) {
            ra.addFlashAttribute("error", "La cantidad del insumo debe ser mayor a 0.");
            return "redirect:/carta";
        }

        // Si el insumo ya está en la receta, actualiza la cantidad
        PlatoInsumo linea = recetaRepo.findByPlatoId(id).stream()
                .filter(pi -> pi.getInsumo().getId_insumo().equals(insumoId))
                .findFirst().orElse(new PlatoInsumo());
        linea.setPlato(plato);
        linea.setInsumo(insumo);
        linea.setCantidad(cantidad);
        recetaRepo.save(linea);

        ra.addFlashAttribute("mensaje", "Receta de \"" + plato.getNombre() + "\" actualizada.");
        return "redirect:/carta";
    }

    /** Quita un ingrediente de la receta. */
    @GetMapping("/receta/eliminar/{lineaId}")
    public String eliminarInsumoReceta(@PathVariable Integer lineaId, RedirectAttributes ra) {
        recetaRepo.findById(lineaId).ifPresent(recetaRepo::delete);
        ra.addFlashAttribute("mensaje", "Ingrediente quitado de la receta.");
        return "redirect:/carta";
    }
}
