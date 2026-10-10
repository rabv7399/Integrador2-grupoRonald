package com.deliciasperuanas.backend.service;

import com.deliciasperuanas.backend.entity.Mesa;
import com.deliciasperuanas.backend.entity.Reserva;
import com.deliciasperuanas.backend.repository.MesaRepository;
import com.deliciasperuanas.backend.repository.ReservaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private MesaRepository mesaRepository;

    @InjectMocks
    private ReservaService reservaService;

    private Mesa mesa;
    private Reserva reserva;

    @BeforeEach
    void setUp() {

        mesa = new Mesa();
        mesa.setId(1L);
        mesa.setNumero(1);
        mesa.setCapacidad(4);
        mesa.setEstado("DISPONIBLE");

        reserva = new Reserva();
        reserva.setCliente("Cliente Prueba");
        reserva.setFecha(LocalDate.of(2026, 10, 10));
        reserva.setHoraInicio(LocalTime.of(19, 0));
        reserva.setHoraFin(LocalTime.of(20, 0));
        reserva.setCantidadPersonas(2);

        Mesa mesaSolicitud = new Mesa();
        mesaSolicitud.setId(1L);

        reserva.setMesa(mesaSolicitud);
    }

    @Test
    void guardarReservaValidaDebeGuardarCorrectamente() {

        when(mesaRepository.findById(1L))
                .thenReturn(Optional.of(mesa));

        when(reservaRepository.buscarConflictos(
                1L,
                reserva.getFecha(),
                reserva.getHoraInicio(),
                reserva.getHoraFin()
        )).thenReturn(List.of());

        when(reservaRepository.save(reserva))
                .thenReturn(reserva);

        Reserva resultado = reservaService.guardar(reserva);

        assertNotNull(resultado);
        assertEquals("PENDIENTE", resultado.getEstado());
        assertSame(mesa, resultado.getMesa());

        verify(reservaRepository, times(1))
                .save(reserva);
    }

    @Test
    void guardarReservaConConflictoDebeRetornar409() {

        when(mesaRepository.findById(1L))
                .thenReturn(Optional.of(mesa));

        when(reservaRepository.buscarConflictos(
                1L,
                reserva.getFecha(),
                reserva.getHoraInicio(),
                reserva.getHoraFin()
        )).thenReturn(List.of(new Reserva()));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reservaService.guardar(reserva)
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(reservaRepository, never())
                .save(any(Reserva.class));
    }

    @Test
    void guardarReservaConHorarioInvalidoDebeRetornar400() {

        reserva.setHoraInicio(LocalTime.of(20, 0));
        reserva.setHoraFin(LocalTime.of(19, 0));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reservaService.guardar(reserva)
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(
                mesaRepository,
                reservaRepository
        );
    }

    @Test
    void guardarReservaConMesaInexistenteDebeRetornar404() {

        Mesa mesaInexistente = new Mesa();
        mesaInexistente.setId(999L);

        reserva.setMesa(mesaInexistente);

        when(mesaRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reservaService.guardar(reserva)
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(reservaRepository, never())
                .save(any(Reserva.class));
    }
}