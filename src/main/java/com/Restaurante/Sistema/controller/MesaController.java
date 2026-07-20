package com.Restaurante.Sistema.controller;

import com.Restaurante.Sistema.entity.Mesa;
import com.Restaurante.Sistema.service.MesaService;
import com.Restaurante.Sistema.service.QRCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/mesas")
public class MesaController {

    private final MesaService mesaService;
    private final QRCodeService qrCodeService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public MesaController(MesaService mesaService, QRCodeService qrCodeService) {
        this.mesaService = mesaService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping
    public String listar(Model model) {
        List<Mesa> mesas = mesaService.listarTodas();
        model.addAttribute("mesas", mesas);
        model.addAttribute("nuevaMesa", new Mesa());
        return "Mesas/mesas";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Mesa mesa, RedirectAttributes ra) {
        try {
            mesaService.guardar(mesa);
            ra.addFlashAttribute("mensaje", "Mesa guardada correctamente.");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Ya existe una mesa con ese número.");
        }
        return "redirect:/mesas";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {
        mesaService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Mesa eliminada correctamente.");
        return "redirect:/mesas";
    }

    @PostMapping("/{id}/estado")
    @ResponseBody
    public ResponseEntity<Void> cambiarEstado(@PathVariable Integer id, @RequestParam String estado) {
        mesaService.listarTodas().stream()
            .filter(m -> m.getId_mesa().equals(id))
            .findFirst()
            .ifPresent(m -> {
                m.setEstado(Mesa.EstadoMesa.valueOf(estado));
                mesaService.guardar(m);
            });
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> descargarQR(@PathVariable Integer id) {
        return mesaService.listarTodas().stream()
            .filter(m -> m.getId_mesa().equals(id))
            .findFirst()
            .map(mesa -> {
                if (mesa.getQrToken() == null) {
                    mesa = mesaService.guardar(mesa);
                }
                String url = baseUrl + "/mesa/" + mesa.getQrToken();
                byte[] qr = qrCodeService.generarQR(url, 300, 300);
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=mesa-" + mesa.getNumero() + ".png")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qr);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
