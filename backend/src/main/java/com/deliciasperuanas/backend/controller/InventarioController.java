package com.deliciasperuanas.backend.controller;

import com.deliciasperuanas.backend.dto.*;
import com.deliciasperuanas.backend.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventario")
public class InventarioController {
    private final InventarioService service;
    public InventarioController(InventarioService service){this.service=service;}

    @GetMapping
    public List<InventarioRespuesta> listar(){return service.listar();}

    @GetMapping("/movimientos")
    public List<MovimientoInventarioRespuesta> movimientos(){return service.historial();}

    @GetMapping("/{productoId}")
    public InventarioRespuesta consultar(@PathVariable Long productoId){
        return service.consultar(productoId);
    }

    @PatchMapping("/{productoId}/reponer")
    public InventarioRespuesta reponer(@PathVariable Long productoId,
        @Valid @RequestBody ReposicionSolicitud s,Authentication auth){
        return service.reponer(productoId,s.cantidad(),auth.getName());
    }

    @PatchMapping("/{productoId}/salida")
    public InventarioRespuesta darSalida(@PathVariable Long productoId,
        @Valid @RequestBody SalidaInventarioSolicitud s,Authentication auth){
        return service.darSalida(productoId,s.cantidad(),s.comentario(),auth.getName());
    }

    @PutMapping("/{productoId}")
    public InventarioRespuesta configurar(@PathVariable Long productoId,
        @Valid @RequestBody InventarioSolicitud s,Authentication auth){
        return service.configurar(productoId,s.totalPreparado(),auth.getName());
    }
}
