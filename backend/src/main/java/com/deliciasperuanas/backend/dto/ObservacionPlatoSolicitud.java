package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.Size;

public record ObservacionPlatoSolicitud(@Size(max=240) String observacion) {}
