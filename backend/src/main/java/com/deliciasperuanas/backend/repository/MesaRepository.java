package com.deliciasperuanas.backend.repository;

import com.deliciasperuanas.backend.entity.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    Optional<Mesa> findByNumero(Integer numero);

    List<Mesa> findByEstado(String estado);
}