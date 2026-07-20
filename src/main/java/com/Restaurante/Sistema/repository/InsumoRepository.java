package com.Restaurante.Sistema.repository;

import com.Restaurante.Sistema.entity.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Integer> {

    /** Insumos cuyo stock actual está en o por debajo del mínimo (inventario crítico). */
    @Query("SELECT i FROM Insumo i WHERE i.stockActual <= i.stockMinimo ORDER BY i.stockActual ASC")
    List<Insumo> findCriticos();
}
