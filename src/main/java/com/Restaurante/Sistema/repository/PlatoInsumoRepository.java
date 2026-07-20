package com.Restaurante.Sistema.repository;

import com.Restaurante.Sistema.entity.PlatoBebida;
import com.Restaurante.Sistema.entity.PlatoInsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlatoInsumoRepository extends JpaRepository<PlatoInsumo, Integer> {

    List<PlatoInsumo> findByPlato(PlatoBebida plato);

    @Query("SELECT pi FROM PlatoInsumo pi WHERE pi.plato.id_item = :idItem")
    List<PlatoInsumo> findByPlatoId(@Param("idItem") Integer idItem);
}
