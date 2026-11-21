package com.deliciasperuanas.backend.repository;

import com.deliciasperuanas.backend.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Optional<Pedido> findByReferenciaOperacion(String referenciaOperacion);
}
