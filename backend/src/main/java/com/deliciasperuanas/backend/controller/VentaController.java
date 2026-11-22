package com.deliciasperuanas.backend.controller;

import com.deliciasperuanas.backend.dto.*;
import com.deliciasperuanas.backend.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/ventas")
public class VentaController {
    private final VentaService servicio;
    public VentaController(VentaService servicio){this.servicio=servicio;}

    @PostMapping("/pedidos/{pedidoId}/cobrar")
    public VentaRespuesta cobrar(@PathVariable Long pedidoId,
                   @Valid @RequestBody CobroSolicitud req,Authentication auth){
        return servicio.cobrar(pedidoId,req,auth.getName());
    }

    @GetMapping("/pedidos/{pedidoId}")
    public VentaRespuesta comprobante(@PathVariable Long pedidoId){
        return servicio.consultarPorPedido(pedidoId);
    }

    @GetMapping("/pedidos/{pedidoId}/cotizacion")
    public Map<String,Object> cotizar(@PathVariable Long pedidoId,
            @RequestParam(defaultValue="EFECTIVO") String medioPago){
        return servicio.cotizar(pedidoId,medioPago);
    }

    @GetMapping("/resumen")
    public Map<String,Object> resumen(@RequestParam(required=false)
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fecha){
        return servicio.resumen(fecha==null?LocalDate.now():fecha);
    }

    @PostMapping("/cuadres")
    public CierreCajaRespuesta registrarCuadre(@Valid @RequestBody CierreCajaSolicitud req,
                                                Authentication auth){
        return servicio.registrarCuadre(req,auth.getName());
    }

    @GetMapping("/cuadres")
    public List<CierreCajaRespuesta> cuadres(){return servicio.listarCuadres();}
}
