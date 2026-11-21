package com.deliciasperuanas.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "inventario_platos")
public class InventarioPlato {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false, unique = true)
    private Producto producto;

    @Column(name = "total_preparado", nullable = false)
    private int totalPreparado;

    @Column(nullable = false)
    private int reservadas;

    @Column(nullable = false)
    private int vendidas;

    public InventarioPlato() {}
    public Long getId() { return id; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public int getTotalPreparado() { return totalPreparado; }
    public void setTotalPreparado(int value) { totalPreparado = value; }
    public int getReservadas() { return reservadas; }
    public void setReservadas(int value) { reservadas = value; }
    public int getVendidas() { return vendidas; }
    public void setVendidas(int value) { vendidas = value; }
    public int disponibles() { return totalPreparado - reservadas - vendidas; }
}
