package com.deliciasperuanas.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record CierreCajaRespuesta(
    Long id,LocalDate fecha,BigDecimal efectivoEsperado,BigDecimal efectivoContado,
    BigDecimal diferencia,String registradoPor,LocalDateTime fechaRegistro
) {}
