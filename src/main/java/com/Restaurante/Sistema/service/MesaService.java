package com.Restaurante.Sistema.service;

import com.Restaurante.Sistema.entity.Mesa;
import com.Restaurante.Sistema.repository.MesaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MesaService {

    private final MesaRepository mesaRepository;

    public MesaService(MesaRepository mesaRepository) {
        this.mesaRepository = mesaRepository;
    }

    public List<Mesa> listarTodas() {
        return mesaRepository.findAll();
    }

    public Mesa guardar(Mesa mesa) {
        if (mesa.getQrToken() == null || mesa.getQrToken().isBlank()) {
            mesa.setQrToken(UUID.randomUUID().toString());
        }
        return mesaRepository.save(mesa);
    }

    public void ocupar(Mesa mesa) {
        mesa.setEstado(Mesa.EstadoMesa.OCUPADA);
        mesaRepository.save(mesa);
    }

    public void liberar(Integer mesaId) {
        mesaRepository.findById(mesaId).ifPresent(m -> {
            m.setEstado(Mesa.EstadoMesa.LIBRE);
            mesaRepository.save(m);
        });
    }

    public void eliminar(Integer id) {
        mesaRepository.deleteById(id);
    }
}
