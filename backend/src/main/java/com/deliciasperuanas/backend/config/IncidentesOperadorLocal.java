package com.deliciasperuanas.backend.config;

import com.deliciasperuanas.backend.entity.Usuario;
import com.deliciasperuanas.backend.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Usuario temporal para la práctica, activado SOLO mediante el perfil s14-local.
 * No se activa en el despliegue normal de Render.
 */
@Configuration
@Profile("s14-local")
public class IncidentesOperadorLocal {
    @Bean
    CommandLineRunner operadorDemostracion(UsuarioRepository usuarios, PasswordEncoder encoder) {
        return args -> {
            String correo = "operador.s14@prueba.local";
            String password = System.getenv("S14_OPERATOR_PASSWORD");
            if (password == null || password.length() < 12) {
                throw new IllegalStateException("Configure S14_OPERATOR_PASSWORD de al menos 12 caracteres solo para el entorno de pruebas");
            }
            {
                Usuario usuario = usuarios.findByCorreo(correo).orElseGet(Usuario::new);
                usuario.setNombre("Operador laboratorio S14");
                usuario.setCorreo(correo);
                usuario.setPassword(encoder.encode(password));
                usuario.setRol("OPERADOR");
                usuario.setActivo(true);
                usuarios.save(usuario);
            }
        };
    }
}