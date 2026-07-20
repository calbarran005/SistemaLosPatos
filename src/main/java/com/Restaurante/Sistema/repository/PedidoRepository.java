package com.Restaurante.Sistema.repository;

import com.Restaurante.Sistema.entity.Mesa;
import com.Restaurante.Sistema.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByEstadoInOrderByFechaDesc(List<Pedido.EstadoPedido> estados);
    Optional<Pedido> findTopByMesaAndEstadoIn(Mesa mesa, List<Pedido.EstadoPedido> estados);
    List<Pedido> findByEstadoOrderByFechaDesc(Pedido.EstadoPedido estado);
    List<Pedido> findTop8ByOrderByFechaDesc();
}
