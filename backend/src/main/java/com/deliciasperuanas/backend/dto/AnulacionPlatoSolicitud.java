package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnulacionPlatoSolicitud(
    @NotBlank @Size(min=5,max=300) String motivo
) {}
