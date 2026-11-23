package com.deliciasperuanas.backend.dto;
import com.deliciasperuanas.backend.entity.Ticket;
import java.time.LocalDateTime;
public record TicketResponse(Long id,String codigo,String titulo,String descripcion,
 String moduloAfectado,LocalDateTime fechaRegistro,String prioridad,
 String estado,String usuarioReporta,String asignadoA) {
 public static TicketResponse from(Ticket t) {
  return new TicketResponse(t.getId(),t.getCodigo(),t.getTitulo(),t.getDescripcion(),
    t.getModuloAfectado(),t.getFechaRegistro(),t.getPrioridad().getNivelNombre(),
    t.getEstado().getNombreEstado(),t.getUsuarioReporta(),
    t.getAsignadoA()==null ? null : t.getAsignadoA().getNombreResponsable());
 }
}