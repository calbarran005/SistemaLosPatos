package com.Restaurante.Sistema.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Ficha técnica / receta: cuánto de un insumo consume un plato al prepararse.
 * Es la lista de materiales (BOM) que enlaza {@link PlatoBebida} con {@link Insumo}.
 */
@Entity
@Table(name = "PlatoInsumo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatoInsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_item", nullable = false)
    private PlatoBebida plato;

    @ManyToOne
    @JoinColumn(name = "id_insumo", nullable = false)
    private Insumo insumo;

    /** Cantidad de insumo (en su unidad de medida) que usa UNA porción del plato. */
    @Column(nullable = false)
    private Double cantidad;
}
