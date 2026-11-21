package com.deliciasperuanas.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name="configuracion_local")
public class ConfiguracionLocal {
    @Id private Long id=1L;
    @Column(nullable=false,length=100) private String nombre="Delicias Peruanas";
    @Column(nullable=false,length=25) private String telefono="966 100 348";
    @Column(name="logo_ruta",length=255) private String logoRuta;
    public Long getId(){return id;}
    public void setId(Long x){id=x;}
    public String getNombre(){return nombre;}
    public void setNombre(String x){nombre=x;}
    public String getTelefono(){return telefono;}
    public void setTelefono(String x){telefono=x;}
    public String getLogoRuta(){return logoRuta;}
    public void setLogoRuta(String x){logoRuta=x;}
}
