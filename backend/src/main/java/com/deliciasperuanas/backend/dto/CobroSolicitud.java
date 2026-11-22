package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CobroSolicitud(
    @DecimalMin("0.01") BigDecimal montoRecibido,
    String medioPago,
    @Size(max=80) String referenciaPago,
    Boolean recargoInformadoAceptado
) {
    public CobroSolicitud(BigDecimal montoRecibido) {
        this(montoRecibido,"EFECTIVO",null,false);
    }
    public CobroSolicitud(BigDecimal montoRecibido,String medioPago,String referenciaPago) {
        this(montoRecibido,medioPago,referenciaPago,false);
    }
}
