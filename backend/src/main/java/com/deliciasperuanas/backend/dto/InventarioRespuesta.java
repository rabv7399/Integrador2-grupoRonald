package com.deliciasperuanas.backend.dto;

public record InventarioRespuesta(
    Long productoId, String producto,
    int totalPreparado, int reservadas, int vendidas, int disponibles,
    int salidasManuales
) {}
