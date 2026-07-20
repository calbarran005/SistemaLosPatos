package com.Restaurante.Sistema.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Mesas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_mesa;

    @Column(nullable = false, unique = true)
    private Integer numero;

    @Column(nullable = false)
    private Integer capacidad;

    @Enumerated(EnumType.STRING)
    private EstadoMesa estado = EstadoMesa.LIBRE;

    @Column(name = "qr_token", unique = true, length = 36)
    private String qrToken;

    public enum EstadoMesa {
        LIBRE, OCUPADA, RESERVADA, MANTENIMIENTO
    }
}
