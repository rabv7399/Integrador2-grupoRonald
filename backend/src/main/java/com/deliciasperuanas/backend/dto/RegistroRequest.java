package com.deliciasperuanas.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        @Pattern(
                regexp = "^[^<>]*$",
                message = "El nombre contiene caracteres no permitidos"
        )
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 150)
        String correo,

        @Pattern(
                regexp = "^$|^[0-9+ -]{6,20}$",
                message = "El telefono no tiene un formato valido"
        )
        String telefono,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(
                min = 8,
                max = 72,
                message = "La contrasena debe tener entre 8 y 72 caracteres"
        )
        String password

) {
}