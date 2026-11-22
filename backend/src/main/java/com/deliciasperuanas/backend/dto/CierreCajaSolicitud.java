package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CierreCajaSolicitud(
    @NotNull LocalDate fecha,
    @NotNull @DecimalMin("0.00") BigDecimal efectivoContado
) {}
