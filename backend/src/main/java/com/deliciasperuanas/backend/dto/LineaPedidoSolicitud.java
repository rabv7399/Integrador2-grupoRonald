package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;

public record LineaPedidoSolicitud(
    @NotNull @Positive Long productoId,
    @NotNull @Positive @Max(20) Integer cantidad
) {}
