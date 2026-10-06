package com.deliciasperuanas.backend.service;

import com.deliciasperuanas.backend.entity.Mesa;
import com.deliciasperuanas.backend.repository.MesaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MesaService {

    private final MesaRepository mesaRepository;

    public MesaService(MesaRepository mesaRepository) {
        this.mesaRepository = mesaRepository;
    }

    public List<Mesa> listar() {
        return mesaRepository.findAll();
    }

    public Mesa buscarPorId(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Mesa no encontrada"
                        )
                );
    }

    public Mesa guardar(Mesa mesa) {
        return mesaRepository.save(mesa);
    }

    public Mesa actualizar(Long id, Mesa datos) {

        Mesa mesa = buscarPorId(id);

        mesa.setNumero(datos.getNumero());
        mesa.setCapacidad(datos.getCapacidad());
        mesa.setEstado(datos.getEstado());

        return mesaRepository.save(mesa);
    }

    public void eliminar(Long id) {
        Mesa mesa = buscarPorId(id);
        mesaRepository.delete(mesa);
    }
}