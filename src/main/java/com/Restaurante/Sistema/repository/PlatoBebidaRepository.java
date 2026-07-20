package com.Restaurante.Sistema.repository;

import com.Restaurante.Sistema.entity.PlatoBebida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatoBebidaRepository extends JpaRepository<PlatoBebida, Integer> {
    long countByDisponibleTrue();
}
