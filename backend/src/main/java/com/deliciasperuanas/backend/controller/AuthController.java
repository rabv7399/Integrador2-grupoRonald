package com.deliciasperuanas.backend.controller;

import com.deliciasperuanas.backend.dto.AuthResponse;
import com.deliciasperuanas.backend.dto.LoginRequest;
import com.deliciasperuanas.backend.dto.RegistroRequest;
import com.deliciasperuanas.backend.dto.UsuarioResponse;
import com.deliciasperuanas.backend.entity.Usuario;
import com.deliciasperuanas.backend.security.JwtService;
import com.deliciasperuanas.backend.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(
            AuthService authService,
            JwtService jwtService
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(
            @Valid @RequestBody RegistroRequest request
    ) {
        return authService.registrar(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        Usuario usuario = authService.autenticar(request);

        String token = jwtService.generarToken(usuario);

        return new AuthResponse(
                token,
                "Bearer",
                usuario.getCorreo(),
                usuario.getRol()
        );
    }
}