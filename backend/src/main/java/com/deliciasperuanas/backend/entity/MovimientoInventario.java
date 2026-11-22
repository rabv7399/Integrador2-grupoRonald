package com.deliciasperuanas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="movimientos_inventario")
public class MovimientoInventario {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="producto_id",nullable=false)
    private Producto producto;

    @Column(nullable=false,length=15)
    private String tipo;

    @Column(nullable=false)
    private int cantidad;

    @Column(nullable=false,length=500)
    private String comentario;

    @Column(name="usuario_correo",nullable=false,length=150)
    private String usuarioCorreo;

    @Column(name="fecha",nullable=false,updatable=false)
    private LocalDateTime fecha=LocalDateTime.now();

    public Long getId(){return id;}
    public Producto getProducto(){return producto;}
    public void setProducto(Producto x){producto=x;}
    public String getTipo(){return tipo;}
    public void setTipo(String x){tipo=x;}
    public int getCantidad(){return cantidad;}
    public void setCantidad(int x){cantidad=x;}
    public String getComentario(){return comentario;}
    public void setComentario(String x){comentario=x;}
    public String getUsuarioCorreo(){return usuarioCorreo;}
    public void setUsuarioCorreo(String x){usuarioCorreo=x;}
    public LocalDateTime getFecha(){return fecha;}
}
