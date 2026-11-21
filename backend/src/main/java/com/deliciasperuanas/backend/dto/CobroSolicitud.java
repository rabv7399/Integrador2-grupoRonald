package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CobroSolicitud(
    @DecimalMin("0.01") BigDecimal montoRecibido,
    String medioPago,
    @Size(max=80) String referenciaPago
) {
    // Los clientes antiguos que solo mandan montoRecibido siguen pagando en EFECTIVO.
    public CobroSolicitud(BigDecimal montoRecibido) {
        this(montoRecibido,"EFECTIVO",null);
    }
}
