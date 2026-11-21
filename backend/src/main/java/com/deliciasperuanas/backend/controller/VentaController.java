package com.deliciasperuanas.backend.controller;

import com.deliciasperuanas.backend.dto.*;
import com.deliciasperuanas.backend.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ventas")
public class VentaController {
    private final VentaService servicio;
    public VentaController(VentaService servicio){this.servicio=servicio;}

    @PostMapping("/pedidos/{pedidoId}/cobrar")
    public VentaRespuesta cobrar(@PathVariable Long pedidoId,
                              @Valid @RequestBody CobroSolicitud req) {
        return servicio.cobrar(pedidoId,req.montoRecibido());
    }

    @GetMapping("/pedidos/{pedidoId}")
    public VentaRespuesta comprobante(@PathVariable Long pedidoId){
        return servicio.consultarPorPedido(pedidoId);
    }

    @GetMapping("/resumen")
    public Map<String,Object> resumen(){return servicio.resumen();}
}
