package com.deliciasperuanas.backend.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record CrearTicketRequest(
 @NotBlank @Size(max=150) String titulo,
 @NotBlank @Size(max=100) String moduloAfectado,
 @NotBlank @Size(max=3000) String descripcion,
 @NotBlank @Pattern(regexp="P[1-4]") String prioridad
) {}