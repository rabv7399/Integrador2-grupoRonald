package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public record CantidadPlatoSolicitud(
    @NotNull @Min(1) @Max(20) Integer cantidad
) {}
