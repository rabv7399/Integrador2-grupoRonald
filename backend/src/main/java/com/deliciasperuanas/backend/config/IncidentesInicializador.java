package com.deliciasperuanas.backend.config;
import com.deliciasperuanas.backend.entity.*;
import com.deliciasperuanas.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class IncidentesInicializador {
 @Bean
 CommandLineRunner catalogosIncidentes(EstadoTicketRepository estados,PrioridadRepository prioridades,AsignadoARepository asignados) {
  return args -> {
   for(String nombre:new String[]{"Abierto","En Diagnóstico","Resuelto","Cerrado"}){
    if(estados.findByNombreEstado(nombre).isEmpty()) estados.save(new EstadoTicket(nombre));
   }
   var catalogo=new Object[][]{{"P1","Crítico",15},{"P2","Alto",60},{"P3","Normal",240},{"P4","Bajo",480}};
   for(Object[] fila:catalogo){
    String nivel=(String)fila[0];
    if(prioridades.findByNivelNombre(nivel).isEmpty())
     prioridades.save(new Prioridad(nivel,(String)fila[1],(Integer)fila[2]));
   }
   if(asignados.count()==0){
    asignados.save(new AsignadoA("Mesa de ayuda",1,"Atención inicial"));
    asignados.save(new AsignadoA("Especialista infraestructura",2,"Servicios y base de datos"));
    asignados.save(new AsignadoA("Equipo de desarrollo",3,"Aplicación Java"));
   }
  };
 }
}