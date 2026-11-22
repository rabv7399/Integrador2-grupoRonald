package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.*;

public record SalidaInventarioSolicitud(
    @NotNull @Min(1) @Max(10000) Integer cantidad,
    @NotBlank @Size(min=5,max=500) String comentario
) {}
