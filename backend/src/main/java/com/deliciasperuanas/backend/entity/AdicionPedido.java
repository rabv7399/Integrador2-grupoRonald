package com.deliciasperuanas.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name="adiciones_pedido",uniqueConstraints=@UniqueConstraint(name="uq_adicion_clave",columnNames="clave"))
public class AdicionPedido {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false,length=100,updatable=false,unique=true)
    private String clave;
    @Column(nullable=false,length=64,updatable=false)
    private String firma;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="pedido_id",nullable=false,updatable=false)
    private Pedido pedido;
    public Long getId(){return id;}
    public String getClave(){return clave;}
    public void setClave(String x){clave=x;}
    public String getFirma(){return firma;}
    public void setFirma(String x){firma=x;}
    public Pedido getPedido(){return pedido;}
    public void setPedido(Pedido x){pedido=x;}
}
