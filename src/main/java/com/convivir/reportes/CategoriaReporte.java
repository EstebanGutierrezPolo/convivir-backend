package com.convivir.reportes;

import java.util.ArrayList;
import java.util.List;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.inmuebles.EstadoRegistro;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoriaReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String nombre;

    private String descripcion;

    @Enumerated(EnumType.STRING)
    private EstadoRegistro estado;

    // CategoriaReporte "1" o-- "*" Reporte : clasifica
    @OneToMany(mappedBy = "categoria")
    private List<Reporte> reportes = new ArrayList<>();

    public CategoriaReporte(String nombre) {
        this.nombre = nombre == null ? null : nombre.trim();
    }

    /** RF-100. */
    public void crearCategoriaDeReporte(String descripcion) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("Escribe el nombre de la categoría.");
        }
        this.descripcion = descripcion;
        this.estado = EstadoRegistro.ACTIVO;
    }

    /** RF-110. */
    public void modificarCategoriaDeReporte(String nombre, String descripcion) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("Escribe el nombre de la categoría.");
        }
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
    }

    /** RF-111. Los reportes que ya la usan la conservan. */
    public void desactivarCategoriaDeReporte() {
        if (estado == EstadoRegistro.INACTIVO) {
            throw new ReglaDeNegocioException("La categoría " + nombre + " ya está desactivada.");
        }
        this.estado = EstadoRegistro.INACTIVO;
    }
}
