package com.Restaurante.Sistema.repository;

import com.Restaurante.Sistema.entity.OrdenCompra;
import com.Restaurante.Sistema.entity.OrdenCompraDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdenCompraDetalleRepository extends JpaRepository<OrdenCompraDetalle, Integer> {
    List<OrdenCompraDetalle> findByOrden(OrdenCompra orden);
}
