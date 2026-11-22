package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

public record LineaPedidoSolicitud(
    @NotNull @Positive Long productoId,
    @NotNull @Positive @Max(20) Integer cantidad,
    @Size(max=240) String observacion
) {
    public LineaPedidoSolicitud(Long productoId,Integer cantidad){
        this(productoId,cantidad,null);
    }
}
