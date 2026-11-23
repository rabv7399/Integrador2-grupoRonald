package com.deliciasperuanas.backend.entity;
import jakarta.persistence.*;
@Entity @Table(name="estado_ticket")
public class EstadoTicket {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=40) private String nombreEstado;
 public EstadoTicket(){}
 public EstadoTicket(String nombre){this.nombreEstado=nombre;}
 public Long getId(){return id;}
 public String getNombreEstado(){return nombreEstado;}
}