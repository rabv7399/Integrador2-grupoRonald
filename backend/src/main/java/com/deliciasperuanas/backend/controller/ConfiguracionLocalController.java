package com.deliciasperuanas.backend.controller;

import com.deliciasperuanas.backend.entity.ConfiguracionLocal;
import com.deliciasperuanas.backend.dto.ConfigLocalSolicitud;
import com.deliciasperuanas.backend.repository.ConfiguracionLocalRepository;
import com.deliciasperuanas.backend.service.OperacionNegocioException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/configuracion")
public class ConfiguracionLocalController {
    private final ConfiguracionLocalRepository repo;
    public ConfiguracionLocalController(ConfiguracionLocalRepository repo){this.repo=repo;}
    @GetMapping
    public ConfiguracionLocal consultar(){return repo.findById(1L).orElseGet(ConfiguracionLocal::new);}

    @PutMapping
    @Transactional
    public ConfiguracionLocal actualizar(@Valid @RequestBody ConfigLocalSolicitud s){
        // Solo logos ubicados dentro de nuestro propio portal. No se aceptan URLs remotas.
        String logo=s.logoRuta()==null?s.logoRuta():s.logoRuta().trim();
        if(logo!=null && !logo.isBlank() &&
            !logo.matches("/portal/images/[a-zA-Z0-9_-]+\\.(png|jpg|jpeg|webp)")) {
            throw new OperacionNegocioException(HttpStatus.BAD_REQUEST,
                "LOGO_RUTA_INVALIDA","El logo debe existir en /portal/images/");
        }
        ConfiguracionLocal local=repo.findById(1L).orElseGet(ConfiguracionLocal::new);
        local.setNombre(s.nombre().trim());
        local.setTelefono(s.telefono().trim());
        local.setLogoRuta(logo);
        return repo.save(local);
    }
}
