package com.deliciasperuanas.backend.repository;
import com.deliciasperuanas.backend.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TicketRepository extends JpaRepository<Ticket,Long> {
 List<Ticket> findAllByOrderByFechaRegistroDesc();
}