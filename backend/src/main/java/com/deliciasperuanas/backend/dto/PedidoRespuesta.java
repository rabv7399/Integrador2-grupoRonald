package com.deliciasperuanas.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoRespuesta(
    Long pedidoId,
    String referenciaOperacion,
    String estado,
    String clienteCorreo,
    BigDecimal total,
    LocalDateTime fechaCreacion,
    List<Item> items
) {
    public record Item(Long productoId, String nombre, int cantidad,
                       BigDecimal precioUnitario, BigDecimal subtotal) {}
}
