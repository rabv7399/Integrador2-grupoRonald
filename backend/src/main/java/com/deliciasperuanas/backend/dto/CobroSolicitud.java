package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record CobroSolicitud(@NotNull @DecimalMin("0.01") BigDecimal montoRecibido) {}
