package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.*;

public record CrearUsuarioAdmin(
    @NotBlank @Size(max=100) String nombre,
    @Email @NotBlank String correo,
    @NotBlank @Size(min=8,max=100) String password,
    @NotBlank String rol,
    String telefono
) {}
