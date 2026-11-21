package com.deliciasperuanas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "detalle_pedidos")
public class DetallePedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "estado_preparacion", nullable = false, length = 15)
    private String estadoPreparacion = "PENDIENTE";

    @Column(name="fecha_inicio", nullable=false)
    private LocalDateTime fechaInicio=LocalDateTime.now();

    @Column(name="fecha_listo")
    private LocalDateTime fechaListo;

    @Column(name="fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    public Long getId() { return id; }
    public LocalDateTime getFechaInicio(){return fechaInicio;}
    public void setFechaInicio(LocalDateTime v){fechaInicio=v;}
    public LocalDateTime getFechaListo(){return fechaListo;}
    public void setFechaListo(LocalDateTime v){fechaListo=v;}
    public LocalDateTime getFechaEntrega(){return fechaEntrega;}
    public void setFechaEntrega(LocalDateTime v){fechaEntrega=v;}
    public String getEstadoPreparacion() { return estadoPreparacion; }
    public void setEstadoPreparacion(String v) { estadoPreparacion = v; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido v) { pedido = v; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto v) { producto = v; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int v) { cantidad = v; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal v) { precioUnitario = v; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal v) { subtotal = v; }
}
