package com.deliciasperuanas.backend.controller;

import com.deliciasperuanas.backend.service.OperacionNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {PedidoController.class, InventarioController.class})
public class ErroresServiciosSoa {
    @ExceptionHandler(OperacionNegocioException.class)
    ResponseEntity<Map<String, String>> negocio(OperacionNegocioException e) {
        return ResponseEntity.status(e.getEstado()).body(Map.of("codigo", e.getCodigo(), "mensaje", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream().findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("Datos de entrada no validos");
        return ResponseEntity.badRequest().body(Map.of("codigo", "DATOS_INVALIDOS", "mensaje", mensaje));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> unicidad(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("codigo", "CONFLICTO_CONCURRENCIA",
                "mensaje", "La operacion ya esta en curso o existe un conflicto de unicidad; consulta su estado antes de reintentar"));
    }
}
