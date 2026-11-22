package com.deliciasperuanas.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name="producto_imagenes")
public class ProductoImagen {
    @Id
    @Column(name="producto_id")
    private Long id;

    @OneToOne(fetch=FetchType.LAZY,optional=false)
    @MapsId
    @JoinColumn(name="producto_id")
    private Producto producto;

    @Lob @Basic(fetch=FetchType.LAZY)
    @Column(name="contenido",nullable=false)
    private byte[] contenido;

    @Column(name="tipo_mime",nullable=false,length=30)
    private String tipoMime;

    public Long getId(){return id;}
    public Producto getProducto(){return producto;}
    public void setProducto(Producto x){producto=x;}
    public byte[] getContenido(){return contenido;}
    public void setContenido(byte[] x){contenido=x;}
    public String getTipoMime(){return tipoMime;}
    public void setTipoMime(String x){tipoMime=x;}
}
