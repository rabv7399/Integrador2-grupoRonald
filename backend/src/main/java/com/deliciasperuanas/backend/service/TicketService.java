package com.deliciasperuanas.backend.service;
import com.deliciasperuanas.backend.dto.*;
import com.deliciasperuanas.backend.entity.*;
import com.deliciasperuanas.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@Service
public class TicketService {
 private final TicketRepository tickets;
 private final PrioridadRepository prioridades;
 private final EstadoTicketRepository estados;
 public TicketService(TicketRepository tickets,PrioridadRepository prioridades,EstadoTicketRepository estados){
  this.tickets=tickets;this.prioridades=prioridades;this.estados=estados;
 }
 @Transactional
 public TicketResponse crear(CrearTicketRequest request,String usuario) {
  Prioridad prioridad=prioridades.findByNivelNombre(request.prioridad())
   .orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Prioridad no registrada"));
  EstadoTicket abierto=estados.findByNombreEstado("Abierto")
   .orElseThrow(()->new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Estado inicial no configurado"));
  Ticket t=new Ticket();
  t.setTitulo(request.titulo().trim());
  t.setModuloAfectado(request.moduloAfectado().trim());
  t.setDescripcion(request.descripcion().trim());
  t.setPrioridad(prioridad);t.setEstado(abierto);t.setUsuarioReporta(usuario);
  t=tickets.saveAndFlush(t);
  t.setCodigo(String.format("INC-%06d",t.getId()));
  return TicketResponse.from(tickets.save(t));
 }
 @Transactional(readOnly=true)
 public List<TicketResponse> listar() {
  return tickets.findAllByOrderByFechaRegistroDesc().stream().map(TicketResponse::from).toList();
 }
}