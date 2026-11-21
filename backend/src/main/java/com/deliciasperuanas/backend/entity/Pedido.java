package com.deliciasperuanas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos", uniqueConstraints = @UniqueConstraint(name = "uq_pedido_referencia", columnNames = "referencia_operacion"))
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "referencia_operacion", nullable = false, length = 100, updatable = false)
    private String referenciaOperacion;

    @Column(name = "firma_solicitud", nullable = false, length = 64, updatable = false)
    private String firmaSolicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    @PrePersist
    void crearFecha() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }

    public void agregarDetalle(DetallePedido d) { d.setPedido(this); detalles.add(d); }
    public Long getId() { return id; }
    public String getReferenciaOperacion() { return referenciaOperacion; }
    public void setReferenciaOperacion(String v) { referenciaOperacion = v; }
    public String getFirmaSolicitud() { return firmaSolicitud; }
    public void setFirmaSolicitud(String v) { firmaSolicitud = v; }
    public Usuario getCliente() { return cliente; }
    public void setCliente(Usuario v) { cliente = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado = v; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal v) { total = v; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public List<DetallePedido> getDetalles() { return detalles; }
}
