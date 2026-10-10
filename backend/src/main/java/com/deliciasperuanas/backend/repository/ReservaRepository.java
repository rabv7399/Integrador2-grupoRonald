package com.deliciasperuanas.backend.repository;

import com.deliciasperuanas.backend.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByFecha(LocalDate fecha);

    @Query("""
        SELECT r
        FROM Reserva r
        WHERE r.mesa.id = :mesaId
          AND r.fecha = :fecha
          AND r.estado <> 'CANCELADA'
          AND r.horaInicio < :horaFin
          AND r.horaFin > :horaInicio
    """)
    List<Reserva> buscarConflictos(
            @Param("mesaId") Long mesaId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin
    );
}