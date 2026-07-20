package com.Restaurante.Sistema.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PlatosYBebidas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatoBebida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_item;

    @ManyToOne
    @JoinColumn(name = "id_categoria", nullable = false)
    private CategoriaMenu categoria;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_venta", nullable = false)
    private Double precioVenta;

    private boolean disponible = true;

    @Column(name = "imagen_url", length = 255)
    private String imagenUrl;
}
