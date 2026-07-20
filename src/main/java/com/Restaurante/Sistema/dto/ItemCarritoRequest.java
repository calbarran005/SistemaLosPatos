package com.Restaurante.Sistema.dto;

import lombok.Data;

@Data
public class ItemCarritoRequest {
    private Integer itemId;
    private Integer cantidad;
}
