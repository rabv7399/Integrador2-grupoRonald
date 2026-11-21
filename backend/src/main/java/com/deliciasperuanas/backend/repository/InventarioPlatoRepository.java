package com.deliciasperuanas.backend.repository;

import com.deliciasperuanas.backend.entity.InventarioPlato;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface InventarioPlatoRepository extends JpaRepository<InventarioPlato, Long> {
    Optional<InventarioPlato> findByProductoId(Long productoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventarioPlato i where i.producto.id = :productoId")
    Optional<InventarioPlato> bloquearProducto(@Param("productoId") Long productoId);
}
