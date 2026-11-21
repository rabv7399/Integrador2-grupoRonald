package com.deliciasperuanas.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CrearPedidoSolicitud(
    @NotEmpty @Size(max = 20) List<@Valid LineaPedidoSolicitud> items
) {}
