package com.deliciasperuanas.backend.dto;

import java.time.LocalDateTime;

public record MovimientoInventarioRespuesta(Long id,Long productoId,String producto,
    String tipo,int cantidad,String comentario,String usuarioCorreo,LocalDateTime fecha) {}
