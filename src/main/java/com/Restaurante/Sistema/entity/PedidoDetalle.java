package com.Restaurante.Sistema.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PedidoDetalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_detalle;

    @ManyToOne
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "id_item_menu", nullable = false)
    private PlatoBebida platoBebida;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Double subtotal;

    @Column(name = "servido", nullable = false)
    private boolean servido = false;
}
