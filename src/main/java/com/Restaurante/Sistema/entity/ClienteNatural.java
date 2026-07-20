package com.Restaurante.Sistema.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Natural_Persona")
@PrimaryKeyJoinColumn(name = "id_cliente")
@Getter
@Setter
@NoArgsConstructor
public class ClienteNatural extends Cliente {

    @Column(nullable = false, unique = true, length = 8)
    private String dni;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellidos;
}
