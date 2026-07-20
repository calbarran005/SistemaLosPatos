package com.Restaurante.Sistema.repository;

import com.Restaurante.Sistema.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

    boolean existsByRuc(String ruc);

    /** ¿Existe otro proveedor (distinto de {@code id}) con este RUC? Para validar al editar. */
    @Query("SELECT COUNT(p) > 0 FROM Proveedor p WHERE p.ruc = :ruc AND p.id_proveedor <> :id")
    boolean existsByRucExcluding(@Param("ruc") String ruc, @Param("id") Integer id);
}
