package com.deliciasperuanas.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaRespuesta(
  Long ventaId,String comprobante,Long pedidoId,String tipoAtencion,Long mesaId,
  String restaurante,String telefono,BigDecimal total,
  BigDecimal montoRecibido,BigDecimal cambio,LocalDateTime fechaPago,
  String medioPago,List<PedidoRespuesta.Item> items,String aviso,
  String referenciaPago,String cajeroCorreo,
  BigDecimal subtotalBase,BigDecimal recargoPos,boolean recargoInformadoAceptado
) {}
