package com.deliciasperuanas.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

// tipoAtencion = MESA o LLEVAR; un cliente solo puede elegir LLEVAR.
public record CrearPedidoSolicitud(
    @NotEmpty @Size(max = 20) List<@Valid LineaPedidoSolicitud> items,
    String tipoAtencion,
    Long mesaId
) {
    // Compatibilidad con los clientes REST y pruebas existentes.
    public CrearPedidoSolicitud(List<LineaPedidoSolicitud> items) {
        this(items, "LLEVAR", null);
    }
}
