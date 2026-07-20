package com.Restaurante.Sistema.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoNotificacion {
    private Integer pedidoId;
    private Integer mesaNumero;
    private String clienteNombre;
    private int cantidadItems;
    private Double total;
    private String tipo; // "NUEVO" | "ACTUALIZADO"
    private String estado;
}
