package com.deliciasperuanas.backend.service;

import com.deliciasperuanas.backend.entity.Mesa;
import com.deliciasperuanas.backend.entity.Reserva;
import com.deliciasperuanas.backend.repository.MesaRepository;
import com.deliciasperuanas.backend.repository.ReservaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final MesaRepository mesaRepository;

    public ReservaService(
            ReservaRepository reservaRepository,
            MesaRepository mesaRepository
    ) {
        this.reservaRepository = reservaRepository;
        this.mesaRepository = mesaRepository;
    }

    public List<Reserva> listar() {
        return reservaRepository.findAll();
    }

    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Reserva no encontrada"
                        )
                );
    }

    @Transactional
    public Reserva guardar(Reserva reserva) {

        if (reserva.getMesa() == null ||
                reserva.getMesa().getId() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar una mesa"
            );
        }

        if (reserva.getFecha() == null ||
                reserva.getHoraInicio() == null ||
                reserva.getHoraFin() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Fecha y horario son obligatorios"
            );
        }

        if (!reserva.getHoraInicio().isBefore(reserva.getHoraFin())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La hora de inicio debe ser anterior a la hora final"
            );
        }

        Mesa mesa = mesaRepository
                .findById(reserva.getMesa().getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Mesa no encontrada"
                        )
                );

        List<Reserva> conflictos =
                reservaRepository.buscarConflictos(
                        mesa.getId(),
                        reserva.getFecha(),
                        reserva.getHoraInicio(),
                        reserva.getHoraFin()
                );

        if (!conflictos.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La mesa no está disponible para el horario solicitado"
            );
        }

        reserva.setMesa(mesa);

        if (reserva.getEstado() == null ||
                reserva.getEstado().isBlank()) {

            reserva.setEstado("PENDIENTE");
        }

        return reservaRepository.save(reserva);
    }

    public void cancelar(Long id) {

        Reserva reserva = buscarPorId(id);

        reserva.setEstado("CANCELADA");

        reservaRepository.save(reserva);
    }
}