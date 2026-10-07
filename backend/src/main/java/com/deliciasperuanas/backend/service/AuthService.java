package com.deliciasperuanas.backend.service;

import com.deliciasperuanas.backend.dto.LoginRequest;
import com.deliciasperuanas.backend.dto.RegistroRequest;
import com.deliciasperuanas.backend.dto.UsuarioResponse;
import com.deliciasperuanas.backend.entity.Usuario;
import com.deliciasperuanas.backend.repository.UsuarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@Transactional
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioResponse registrar(RegistroRequest request) {

        String correo = request.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya se encuentra registrado"
            );
        }

        Usuario usuario = new Usuario();

        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setTelefono(
                request.telefono() == null || request.telefono().isBlank()
                        ? null
                        : request.telefono().trim()
        );

        // BCrypt: nunca se almacena la contraseña en texto plano.
        usuario.setPassword(
                passwordEncoder.encode(request.password())
        );

        // El registro público siempre crea un CLIENTE.
        usuario.setRol("CLIENTE");
        usuario.setActivo(true);

        Usuario guardado = usuarioRepository.save(usuario);

        return convertirAResponse(guardado);
    }

    @Transactional(readOnly = true)
    public Usuario autenticar(LoginRequest request) {

        String correo = request.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Credenciales incorrectas"
                ));

        if (!passwordEncoder.matches(
                request.password(),
                usuario.getPassword()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Credenciales incorrectas"
            );
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario se encuentra inactivo"
            );
        }

        return usuario;
    }

    private UsuarioResponse convertirAResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getRol(),
                usuario.getActivo()
        );
    }
}