package com.deliciasperuanas.backend.dto;

public record AuthResponse(
        String token,
        String tipo,
        String correo,
        String rol
) {
}