package com.deliciasperuanas.backend.dto;

public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String telefono,
        String rol,
        Boolean activo
) {
}