package com.Restaurante.Sistema.repository;

import com.Restaurante.Sistema.entity.Pedido;
import com.Restaurante.Sistema.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    Optional<Ticket> findByPedido(Pedido pedido);

    @Query(value = "SELECT * FROM Tickets ORDER BY id_ticket DESC LIMIT 1", nativeQuery = true)
    Optional<Ticket> findUltimoTicket();

    @Query("SELECT t FROM Ticket t ORDER BY t.fechaEmision DESC")
    List<Ticket> findAllOrdenadosPorFecha();

    /**
     * Búsqueda paginada con filtro opcional por método de pago y texto libre
     * sobre la serie del comprobante. Un parámetro {@code null} desactiva ese filtro.
     */
    @Query("SELECT t FROM Ticket t WHERE " +
           "(:metodo IS NULL OR LOWER(t.metodoPago) = LOWER(:metodo)) AND " +
           "(:q IS NULL OR LOWER(t.serieNumero) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Ticket> buscar(@Param("metodo") String metodo, @Param("q") String q, Pageable pageable);

    /** Métodos de pago distintos existentes (para poblar el desplegable de filtro). */
    @Query("SELECT DISTINCT t.metodoPago FROM Ticket t WHERE t.metodoPago IS NOT NULL ORDER BY t.metodoPago")
    List<String> findMetodosPago();

    /** Ingreso total acumulado de todos los comprobantes. */
    @Query("SELECT COALESCE(SUM(t.total), 0) FROM Ticket t")
    double sumTotalIngresos();

    /** Resumen [metodoPago, cantidad, sumaTotal] agrupado por método de pago. */
    @Query("SELECT t.metodoPago, COUNT(t), COALESCE(SUM(t.total), 0) " +
           "FROM Ticket t GROUP BY t.metodoPago ORDER BY SUM(t.total) DESC")
    List<Object[]> resumenPorMetodoPago();

    /** Comprobantes emitidos a partir de una fecha (para métricas del dashboard). */
    List<Ticket> findByFechaEmisionAfter(LocalDateTime fecha);
}
