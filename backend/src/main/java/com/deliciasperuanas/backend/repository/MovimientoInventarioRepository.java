package com.deliciasperuanas.backend.repository;

import com.deliciasperuanas.backend.entity.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario,Long> {
    List<MovimientoInventario> findAllByOrderByIdDesc();
}
