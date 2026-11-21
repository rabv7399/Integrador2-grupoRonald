package com.deliciasperuanas.backend.repository;

import com.deliciasperuanas.backend.entity.CierreCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface CierreCajaRepository extends JpaRepository<CierreCaja,Long> {
    Optional<CierreCaja> findByFecha(LocalDate fecha);
    List<CierreCaja> findAllByOrderByFechaDesc();
}
