package com.Restaurante.Sistema.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Insumos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_insumo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "unidad_medida", length = 20)
    private String unidadMedida;

    @Column(name = "stock_actual")
    private Double stockActual = 0.0;

    @Column(name = "stock_minimo")
    private Double stockMinimo = 0.0;
}
