package com.Restaurante.Sistema.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "OrdenCompraDetalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_detalle;

    @ManyToOne
    @JoinColumn(name = "id_orden", nullable = false)
    private OrdenCompra orden;

    @ManyToOne
    @JoinColumn(name = "id_insumo", nullable = false)
    private Insumo insumo;

    /** Cantidad comprada, en la unidad de medida del insumo. */
    @Column(nullable = false)
    private Double cantidad;

    @Column(name = "costo_unitario", nullable = false)
    private Double costoUnitario;

    @Column(nullable = false)
    private Double subtotal;
}
