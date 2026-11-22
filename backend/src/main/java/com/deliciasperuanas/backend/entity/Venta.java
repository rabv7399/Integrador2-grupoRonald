package com.deliciasperuanas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas", uniqueConstraints = @UniqueConstraint(
    name="uq_ventas_metodo_referencia",columnNames={"medio_pago","referencia_pago"}))
public class Venta {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="pedido_id", nullable=false, unique=true)
    private Pedido pedido;

    @Column(name="numero_comprobante", nullable=false, unique=true, length=60)
    private String numeroComprobante;

    @Column(nullable=false, precision=12, scale=2)
    private BigDecimal total;

    @Column(name="subtotal_base",nullable=false,precision=12,scale=2)
    private BigDecimal subtotalBase=BigDecimal.ZERO;

    @Column(name="recargo_pos",nullable=false,precision=12,scale=2)
    private BigDecimal recargoPos=BigDecimal.ZERO;

    @Column(name="recargo_informado_aceptado",nullable=false)
    private boolean recargoInformadoAceptado=false;

    @Column(name="monto_recibido", nullable=false, precision=12, scale=2)
    private BigDecimal montoRecibido;

    @Column(nullable=false, precision=12, scale=2)
    private BigDecimal cambio;

    @Column(name="fecha_pago", nullable=false)
    private LocalDateTime fechaPago=LocalDateTime.now(java.time.ZoneId.of("America/Lima"));

    @Column(name="medio_pago", nullable=false, length=20)
    private String medioPago="EFECTIVO";

    @Column(name="referencia_pago",length=80)
    private String referenciaPago;

    @Column(name="cajero_correo",nullable=false,length=150)
    private String cajeroCorreo="sistema";

    public Long getId(){return id;}
    public Pedido getPedido(){return pedido;}
    public void setPedido(Pedido p){pedido=p;}
    public String getNumeroComprobante(){return numeroComprobante;}
    public void setNumeroComprobante(String x){numeroComprobante=x;}
    public BigDecimal getSubtotalBase(){return subtotalBase;}
    public void setSubtotalBase(BigDecimal v){subtotalBase=v;}
    public BigDecimal getRecargoPos(){return recargoPos;}
    public void setRecargoPos(BigDecimal v){recargoPos=v;}
    public boolean isRecargoInformadoAceptado(){return recargoInformadoAceptado;}
    public void setRecargoInformadoAceptado(boolean v){recargoInformadoAceptado=v;}
    public BigDecimal getTotal(){return total;}
    public void setTotal(BigDecimal x){total=x;}
    public BigDecimal getMontoRecibido(){return montoRecibido;}
    public void setMontoRecibido(BigDecimal x){montoRecibido=x;}
    public BigDecimal getCambio(){return cambio;}
    public void setCambio(BigDecimal x){cambio=x;}
    public LocalDateTime getFechaPago(){return fechaPago;}
    public void setFechaPago(LocalDateTime x){fechaPago=x;}
    public String getMedioPago(){return medioPago;}
    public void setMedioPago(String x){medioPago=x;}
    public String getReferenciaPago(){return referenciaPago;}
    public void setReferenciaPago(String x){referenciaPago=x;}
    public String getCajeroCorreo(){return cajeroCorreo;}
    public void setCajeroCorreo(String x){cajeroCorreo=x;}
}
