package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.*;
public record EditarUsuarioAdmin(
    @NotBlank @Size(max=100) String nombre,
    @NotBlank String rol,
    Boolean activo,
    String telefono
) {}
