package com.Restaurante.Sistema.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Juridico_Persona")
@PrimaryKeyJoinColumn(name = "id_cliente")
@Getter
@Setter
@NoArgsConstructor
public class ClienteJuridico extends Cliente {

    @Column(nullable = false, unique = true, length = 11)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(length = 100)
    private String representante;
}
