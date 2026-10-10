package com.deliciasperuanas.backend.controller;

import com.deliciasperuanas.backend.entity.Mesa;
import com.deliciasperuanas.backend.service.MesaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mesas")
public class MesaController {

    private final MesaService mesaService;

    public MesaController(MesaService mesaService) {
        this.mesaService = mesaService;
    }

    @GetMapping
    public List<Mesa> listar() {
        return mesaService.listar();
    }

    @GetMapping("/{id}")
    public Mesa buscarPorId(@PathVariable Long id) {
        return mesaService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Mesa> crear(@RequestBody Mesa mesa) {
        Mesa nueva = mesaService.guardar(mesa);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    @PutMapping("/{id}")
    public Mesa actualizar(
            @PathVariable Long id,
            @RequestBody Mesa mesa) {

        return mesaService.actualizar(id, mesa);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        mesaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}