package com.deliciasperuanas.backend.entity;
import jakarta.persistence.*;
@Entity @Table(name="asignado_a")
public class AsignadoA {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=100) private String nombreResponsable;
 @Column(nullable=false) private Integer nivelSoporte;
 @Column(length=100) private String especialidad;
 public AsignadoA(){}
 public AsignadoA(String nombre,Integer nivel,String especialidad){this.nombreResponsable=nombre;this.nivelSoporte=nivel;this.especialidad=especialidad;}
 public Long getId(){return id;}
 public String getNombreResponsable(){return nombreResponsable;}
 public Integer getNivelSoporte(){return nivelSoporte;}
 public String getEspecialidad(){return especialidad;}
}