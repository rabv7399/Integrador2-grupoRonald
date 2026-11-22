package com.deliciasperuanas.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record AdicionPedidoSolicitud(
    @NotEmpty @Size(max=20) List<@Valid LineaPedidoSolicitud> items
) {}
