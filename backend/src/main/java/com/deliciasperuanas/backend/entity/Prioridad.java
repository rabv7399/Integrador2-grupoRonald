package com.deliciasperuanas.backend.entity;
import jakarta.persistence.*;
@Entity @Table(name="prioridad")
public class Prioridad {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=2) private String nivelNombre;
 @Column(nullable=false,length=40) private String descripcion;
 @Column(nullable=false) private Integer tiempoSlaMinutos;
 public Prioridad(){}
 public Prioridad(String nivel,String descripcion,Integer minutos){this.nivelNombre=nivel;this.descripcion=descripcion;this.tiempoSlaMinutos=minutos;}
 public Long getId(){return id;}
 public String getNivelNombre(){return nivelNombre;}
 public String getDescripcion(){return descripcion;}
 public Integer getTiempoSlaMinutos(){return tiempoSlaMinutos;}
}