package com.deliciasperuanas.backend.service;

import org.springframework.http.HttpStatus;

public class OperacionNegocioException extends RuntimeException {
    private final HttpStatus estado;
    private final String codigo;

    public OperacionNegocioException(HttpStatus estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public HttpStatus getEstado() { return estado; }
    public String getCodigo() { return codigo; }
}
