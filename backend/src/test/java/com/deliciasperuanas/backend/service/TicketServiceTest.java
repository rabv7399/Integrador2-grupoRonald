package com.deliciasperuanas.backend.service;

import com.deliciasperuanas.backend.dto.CrearTicketRequest;
import com.deliciasperuanas.backend.entity.*;
import com.deliciasperuanas.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
 @Mock TicketRepository tickets;
 @Mock PrioridadRepository prioridades;
 @Mock EstadoTicketRepository estados;
 @InjectMocks TicketService servicio;

 @Test void crearTicketAsignaCodigoPrioridadEstadoYUsuario() {
  when(prioridades.findByNivelNombre("P1")).thenReturn(Optional.of(new Prioridad("P1","Crítico",15)));
  when(estados.findByNombreEstado("Abierto")).thenReturn(Optional.of(new EstadoTicket("Abierto")));
  when(tickets.saveAndFlush(any(Ticket.class))).thenAnswer(inv -> {
   Ticket t=inv.getArgument(0);ReflectionTestUtils.setField(t,"id",1L);
   ReflectionTestUtils.setField(t,"fechaRegistro",java.time.LocalDateTime.now());
   return t;
  });
  when(tickets.save(any(Ticket.class))).thenAnswer(inv->inv.getArgument(0));
  var creado=servicio.crear(new CrearTicketRequest("Error API","API","No responde", "P1"),"operador@test.pe");
  assertEquals("INC-000001",creado.codigo());
  assertEquals("P1",creado.prioridad());
  assertEquals("Abierto",creado.estado());
  assertEquals("operador@test.pe",creado.usuarioReporta());
  assertNotNull(creado.fechaRegistro());
  verify(tickets).saveAndFlush(any(Ticket.class));
 }
}