package com.deliciasperuanas.backend.controller;

import com.deliciasperuanas.backend.dto.InventarioRespuesta;
import com.deliciasperuanas.backend.dto.InventarioSolicitud;
import com.deliciasperuanas.backend.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventario")
public class InventarioController {
    private final InventarioService service;
    public InventarioController(InventarioService service) { this.service = service; }

    @GetMapping("/{productoId}")
    public InventarioRespuesta consultar(@PathVariable Long productoId) {
        return service.consultar(productoId);
    }

    @PutMapping("/{productoId}")
    public InventarioRespuesta configurar(@PathVariable Long productoId,
                                           @Valid @RequestBody InventarioSolicitud solicitud) {
        return service.configurar(productoId, solicitud.totalPreparado());
    }
}
