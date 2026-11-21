package com.deliciasperuanas.backend.repository;

import com.deliciasperuanas.backend.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta,Long> {
    Optional<Venta> findByPedidoId(Long pedidoId);
    boolean existsByPedidoId(Long pedidoId);
    boolean existsByMedioPagoAndReferenciaPago(String medioPago,String referenciaPago);
}
