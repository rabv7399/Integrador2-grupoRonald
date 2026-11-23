package com.deliciasperuanas.backend.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="ticket")
public class Ticket {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(unique=true,length=20) private String codigo;
 @Column(nullable=false,length=150) private String titulo;
 @Column(nullable=false,columnDefinition="TEXT") private String descripcion;
 @Column(nullable=false,length=100) private String moduloAfectado;
 @Column(nullable=false,updatable=false) private LocalDateTime fechaRegistro;
 @Column(nullable=false,length=150) private String usuarioReporta;
 @ManyToOne(optional=false) @JoinColumn(name="estado_id",nullable=false) private EstadoTicket estado;
 @ManyToOne(optional=false) @JoinColumn(name="prioridad_id",nullable=false) private Prioridad prioridad;
 @ManyToOne @JoinColumn(name="asignado_a_id") private AsignadoA asignadoA;
 @PrePersist void iniciarFecha(){if(fechaRegistro==null) fechaRegistro=LocalDateTime.now();}
 public Long getId(){return id;} public String getCodigo(){return codigo;}
 public void setCodigo(String x){codigo=x;}
 public String getTitulo(){return titulo;} public void setTitulo(String x){titulo=x;}
 public String getDescripcion(){return descripcion;} public void setDescripcion(String x){descripcion=x;}
 public String getModuloAfectado(){return moduloAfectado;} public void setModuloAfectado(String x){moduloAfectado=x;}
 public LocalDateTime getFechaRegistro(){return fechaRegistro;}
 public String getUsuarioReporta(){return usuarioReporta;} public void setUsuarioReporta(String x){usuarioReporta=x;}
 public EstadoTicket getEstado(){return estado;} public void setEstado(EstadoTicket x){estado=x;}
 public Prioridad getPrioridad(){return prioridad;} public void setPrioridad(Prioridad x){prioridad=x;}
 public AsignadoA getAsignadoA(){return asignadoA;} public void setAsignadoA(AsignadoA x){asignadoA=x;}
}