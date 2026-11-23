package com.deliciasperuanas.backend.repository;
import com.deliciasperuanas.backend.entity.Prioridad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PrioridadRepository extends JpaRepository<Prioridad,Long> {
 Optional<Prioridad> findByNivelNombre(String nivelNombre);
}