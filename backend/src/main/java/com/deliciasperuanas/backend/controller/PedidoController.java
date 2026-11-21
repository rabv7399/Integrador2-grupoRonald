package com.deliciasperuanas.backend.controller;

import com.deliciasperuanas.backend.dto.CrearPedidoSolicitud;
import com.deliciasperuanas.backend.dto.PedidoRespuesta;
import com.deliciasperuanas.backend.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {
    private final PedidoService service;
    public PedidoController(PedidoService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<PedidoRespuesta> registrar(
        @RequestHeader(name = "Idempotency-Key") String referencia,
        @Valid @RequestBody CrearPedidoSolicitud solicitud,
        Authentication autenticacion
    ) {
        PedidoService.ResultadoRegistro result = service.registrar(solicitud, referencia, autenticacion.getName());
        return ResponseEntity.status(result.creado() ? HttpStatus.CREATED : HttpStatus.OK)
            .body(result.respuesta());
    }

    @GetMapping
    public java.util.List<PedidoRespuesta> listar(Authentication auth) {
        return service.listar(auth.getName());
    }

    @PostMapping("/{id}/detalles/{detalleId}/listo")
    public PedidoRespuesta marcarListo(@PathVariable Long id,@PathVariable Long detalleId,
                                       Authentication auth) {
        return service.marcarListo(id,detalleId,auth.getName());
    }

    @GetMapping("/{id}")
    public PedidoRespuesta consultar(@PathVariable Long id, Authentication auth) {
        return service.consultar(id, auth.getName());
    }

    @PostMapping("/{id}/confirmacion")
    public PedidoRespuesta confirmar(@PathVariable Long id, Authentication auth) {
        return service.confirmar(id, auth.getName());
    }

    @PostMapping("/{id}/cancelacion")
    public PedidoRespuesta cancelar(@PathVariable Long id, Authentication auth) {
        return service.cancelar(id, auth.getName());
    }
}
