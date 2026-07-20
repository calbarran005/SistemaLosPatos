package com.Restaurante.Sistema.repository;

import com.Restaurante.Sistema.entity.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MesaRepository extends JpaRepository<Mesa, Integer> {
    Optional<Mesa> findByQrToken(String qrToken);
    Optional<Mesa> findByNumero(Integer numero);
    long countByEstado(Mesa.EstadoMesa estado);
}
