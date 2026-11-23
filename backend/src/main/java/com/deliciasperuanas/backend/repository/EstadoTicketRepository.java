package com.deliciasperuanas.backend.repository;
import com.deliciasperuanas.backend.entity.EstadoTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface EstadoTicketRepository extends JpaRepository<EstadoTicket,Long> {
 Optional<EstadoTicket> findByNombreEstado(String nombreEstado);
}