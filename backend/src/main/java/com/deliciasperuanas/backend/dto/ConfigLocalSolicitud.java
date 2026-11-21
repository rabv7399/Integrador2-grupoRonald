package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.*;

public record ConfigLocalSolicitud(
    @NotBlank @Size(max=100) String nombre,
    @NotBlank @Size(max=25) String telefono,
    @Size(max=255) String logoRuta
) {}
