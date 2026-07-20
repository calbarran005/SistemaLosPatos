package com.Restaurante.Sistema.dto;

import lombok.Data;
import java.util.List;

@Data
public class PedidoRequest {
    private List<ItemCarritoRequest> items;
}
