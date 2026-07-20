package com.Restaurante.Sistema.repository;

import com.Restaurante.Sistema.entity.Pedido;
import com.Restaurante.Sistema.entity.PedidoDetalle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoDetalleRepository extends JpaRepository<PedidoDetalle, Integer> {
    List<PedidoDetalle> findByPedido(Pedido pedido);

    /** Ranking de platos/bebidas más vendidos: [nombre, totalUnidades]. */
    @Query("SELECT d.platoBebida.nombre, SUM(d.cantidad) FROM PedidoDetalle d " +
           "GROUP BY d.platoBebida.nombre ORDER BY SUM(d.cantidad) DESC")
    List<Object[]> topPlatos(Pageable pageable);
}
