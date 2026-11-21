package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record ReposicionSolicitud(@NotNull @Min(1) Integer cantidad) {}
