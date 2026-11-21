package com.deliciasperuanas.backend.controller;

import com.deliciasperuanas.backend.dto.*;
import com.deliciasperuanas.backend.entity.Usuario;
import com.deliciasperuanas.backend.repository.UsuarioRepository;
import com.deliciasperuanas.backend.service.OperacionNegocioException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuariosAdminController {
    private static final List<String> ROLES=List.of(
        "CLIENTE","MOZO","CAJA","COCINERO","SUPERVISOR","ADMIN","OPERADOR");
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    public UsuariosAdminController(UsuarioRepository usuarios,PasswordEncoder encoder){
        this.usuarios=usuarios;this.encoder=encoder;
    }

    @GetMapping
    @Transactional(readOnly=true)
    public List<UsuarioResponse> listar(){
        return usuarios.findAll().stream().map(this::dto).toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioAdmin s){
        String correo=s.correo().trim().toLowerCase(Locale.ROOT);
        validarRol(s.rol());
        if(usuarios.existsByCorreo(correo))throw error(HttpStatus.CONFLICT,
            "CORREO_REGISTRADO","Ya existe un usuario con este correo");
        Usuario u=new Usuario();
        u.setNombre(s.nombre().trim());u.setCorreo(correo);
        u.setRol(s.rol());u.setTelefono(s.telefono());
        u.setPassword(encoder.encode(s.password()));u.setActivo(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto(usuarios.save(u)));
    }

    @PutMapping("/{id}")
    @Transactional
    public UsuarioResponse actualizar(@PathVariable Long id,
            @Valid @RequestBody EditarUsuarioAdmin s,Authentication auth) {
        validarRol(s.rol());
        Usuario u=buscar(id);
        evitarDesactivarAdminActual(u,auth,s.activo(),s.rol());
        u.setNombre(s.nombre().trim());
        u.setTelefono(s.telefono());
        u.setRol(s.rol());
        if(s.activo()!=null)u.setActivo(s.activo());
        return dto(u);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public UsuarioResponse darDeBaja(@PathVariable Long id,Authentication auth){
        Usuario u=buscar(id);
        evitarDesactivarAdminActual(u,auth,false,u.getRol());
        // Baja lógica: preservar pedidos, reservas y auditoría.
        u.setActivo(false);
        return dto(u);
    }

    private Usuario buscar(Long id){
        return usuarios.findById(id).orElseThrow(()->
            error(HttpStatus.NOT_FOUND,"USUARIO_NO_ENCONTRADO","No se encontró el usuario"));
    }

    private void evitarDesactivarAdminActual(Usuario u,Authentication auth,
                                             Boolean activo,String rol){
        if(u.getCorreo().equalsIgnoreCase(auth.getName()) &&
                (Boolean.FALSE.equals(activo)||!"ADMIN".equals(rol))) {
            throw error(HttpStatus.CONFLICT,"ADMIN_AUTOGESTION",
                "No puedes desactivar o quitarte el rol ADMIN desde tu propia sesión");
        }
    }

    private void validarRol(String rol){
        if(!ROLES.contains(rol))throw error(HttpStatus.BAD_REQUEST,
            "ROL_INVALIDO","Usa uno de los roles autorizados");
    }
    private UsuarioResponse dto(Usuario u){
        return new UsuarioResponse(u.getId(),u.getNombre(),u.getCorreo(),
            u.getTelefono(),u.getRol(),u.getActivo());
    }
    private OperacionNegocioException error(HttpStatus s,String c,String m){
        return new OperacionNegocioException(s,c,m);
    }
}
