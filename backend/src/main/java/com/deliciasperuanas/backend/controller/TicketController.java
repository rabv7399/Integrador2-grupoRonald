package com.deliciasperuanas.backend.controller;
import com.deliciasperuanas.backend.dto.*;
import com.deliciasperuanas.backend.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/incidentes")
public class TicketController {
 private final TicketService servicio;
 public TicketController(TicketService servicio){this.servicio=servicio;}
 @PostMapping
 public ResponseEntity<TicketResponse> crear(@Valid @RequestBody CrearTicketRequest solicitud,Authentication usuario){
  return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(solicitud,usuario.getName()));
 }
 @GetMapping
 public List<TicketResponse> listar(){return servicio.listar();}
}