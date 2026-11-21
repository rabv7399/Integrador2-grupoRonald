package com.deliciasperuanas.backend.repository;

import com.deliciasperuanas.backend.entity.AdicionPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AdicionPedidoRepository extends JpaRepository<AdicionPedido,Long>{
    Optional<AdicionPedido> findByClave(String clave);
}
