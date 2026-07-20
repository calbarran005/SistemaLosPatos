package com.Restaurante.Sistema.controller;

import com.Restaurante.Sistema.entity.Cliente;
import com.Restaurante.Sistema.entity.ClienteJuridico;
import com.Restaurante.Sistema.entity.ClienteNatural;
import com.Restaurante.Sistema.exception.ResourceNotFoundException;
import com.Restaurante.Sistema.repository.ClienteJuridicoRepository;
import com.Restaurante.Sistema.repository.ClienteNaturalRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteNaturalRepository naturalRepo;
    private final ClienteJuridicoRepository juridicoRepo;

    public ClienteController(ClienteNaturalRepository naturalRepo, ClienteJuridicoRepository juridicoRepo) {
        this.naturalRepo = naturalRepo;
        this.juridicoRepo = juridicoRepo;
    }

    @GetMapping
    public String listar(Model model) {
        List<ClienteNatural> naturales = naturalRepo.findAll();
        List<ClienteJuridico> juridicos = juridicoRepo.findAll();
        model.addAttribute("naturales", naturales);
        model.addAttribute("juridicos", juridicos);

        // Índice ligero {id, text} que Alpine usa para filtrar y paginar en el cliente
        model.addAttribute("natIndex", naturales.stream().map(n -> Map.<String, Object>of(
                "id", n.getId_cliente(),
                "text", (safe(n.getNombre()) + " " + safe(n.getApellidos()) + " " + safe(n.getDni())
                        + " " + safe(n.getTelefono()) + " " + safe(n.getEmail())).toLowerCase()
        )).toList());
        model.addAttribute("jurIndex", juridicos.stream().map(j -> Map.<String, Object>of(
                "id", j.getId_cliente(),
                "text", (safe(j.getRazonSocial()) + " " + safe(j.getRuc()) + " " + safe(j.getRepresentante())
                        + " " + safe(j.getTelefono()) + " " + safe(j.getEmail())).toLowerCase()
        )).toList());

        return "Clientes/clientes";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @PostMapping("/natural/guardar")
    public String guardarNatural(@ModelAttribute ClienteNatural natural, RedirectAttributes ra) {
        try {
            boolean isNew = natural.getId_cliente() == null;
            if (isNew) {
                natural.setTipoCliente(Cliente.TipoCliente.NATURAL);
                naturalRepo.save(natural);
                ra.addFlashAttribute("mensaje", "Cliente registrado correctamente.");
            } else {
                ClienteNatural existing = naturalRepo.findById(natural.getId_cliente())
                        .orElseThrow(() -> new ResourceNotFoundException("Cliente natural", "id", natural.getId_cliente()));
                existing.setNombre(natural.getNombre());
                existing.setApellidos(natural.getApellidos());
                existing.setDni(natural.getDni());
                existing.setTelefono(natural.getTelefono());
                existing.setEmail(natural.getEmail());
                naturalRepo.save(existing);
                ra.addFlashAttribute("mensaje", "Cliente actualizado correctamente.");
            }
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Ya existe un cliente con ese DNI. Verifique los datos.");
        }
        return "redirect:/clientes";
    }

    @PostMapping("/juridico/guardar")
    public String guardarJuridico(@ModelAttribute ClienteJuridico juridico, RedirectAttributes ra) {
        try {
            boolean isNew = juridico.getId_cliente() == null;
            if (isNew) {
                juridico.setTipoCliente(Cliente.TipoCliente.JURIDICO);
                juridicoRepo.save(juridico);
                ra.addFlashAttribute("mensaje", "Empresa registrada correctamente.");
            } else {
                ClienteJuridico existing = juridicoRepo.findById(juridico.getId_cliente())
                        .orElseThrow(() -> new ResourceNotFoundException("Cliente jurídico", "id", juridico.getId_cliente()));
                existing.setRuc(juridico.getRuc());
                existing.setRazonSocial(juridico.getRazonSocial());
                existing.setRepresentante(juridico.getRepresentante());
                existing.setTelefono(juridico.getTelefono());
                existing.setEmail(juridico.getEmail());
                juridicoRepo.save(existing);
                ra.addFlashAttribute("mensaje", "Empresa actualizada correctamente.");
            }
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Ya existe una empresa con ese RUC. Verifique los datos.");
        }
        return "redirect:/clientes";
    }

    @GetMapping("/natural/eliminar/{id}")
    public String eliminarNatural(@PathVariable Integer id, RedirectAttributes ra) {
        naturalRepo.deleteById(id);
        ra.addFlashAttribute("mensaje", "Cliente eliminado correctamente.");
        return "redirect:/clientes";
    }

    @GetMapping("/juridico/eliminar/{id}")
    public String eliminarJuridico(@PathVariable Integer id, RedirectAttributes ra) {
        juridicoRepo.deleteById(id);
        ra.addFlashAttribute("mensaje", "Cliente eliminado correctamente.");
        return "redirect:/clientes";
    }
}
