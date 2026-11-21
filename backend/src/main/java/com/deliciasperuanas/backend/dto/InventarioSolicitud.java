package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventarioSolicitud(@NotNull @Min(0) Integer totalPreparado) {}
