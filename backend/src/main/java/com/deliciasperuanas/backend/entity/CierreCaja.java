package com.deliciasperuanas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="cierres_caja",uniqueConstraints=@UniqueConstraint(name="uq_cierre_dia",columnNames="fecha"))
public class CierreCaja {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false,unique=true)
    private LocalDate fecha;
    @Column(name="efectivo_esperado",nullable=false,precision=12,scale=2)
    private BigDecimal efectivoEsperado;
    @Column(name="efectivo_contado",nullable=false,precision=12,scale=2)
    private BigDecimal efectivoContado;
    @Column(nullable=false,precision=12,scale=2)
    private BigDecimal diferencia;
    @Column(name="registrado_por",nullable=false,length=150)
    private String registradoPor;
    @Column(name="fecha_registro",nullable=false)
    private LocalDateTime fechaRegistro=LocalDateTime.now();
    public Long getId(){return id;}
    public LocalDate getFecha(){return fecha;}
    public void setFecha(LocalDate x){fecha=x;}
    public BigDecimal getEfectivoEsperado(){return efectivoEsperado;}
    public void setEfectivoEsperado(BigDecimal x){efectivoEsperado=x;}
    public BigDecimal getEfectivoContado(){return efectivoContado;}
    public void setEfectivoContado(BigDecimal x){efectivoContado=x;}
    public BigDecimal getDiferencia(){return diferencia;}
    public void setDiferencia(BigDecimal x){diferencia=x;}
    public String getRegistradoPor(){return registradoPor;}
    public void setRegistradoPor(String x){registradoPor=x;}
    public LocalDateTime getFechaRegistro(){return fechaRegistro;}
}
