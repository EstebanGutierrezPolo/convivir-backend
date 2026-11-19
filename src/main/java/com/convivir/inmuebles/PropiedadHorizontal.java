package com.convivir.inmuebles;

import java.util.ArrayList;
import java.util.List;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.eventos.Auditoria;
import com.convivir.eventos.ConfiguracionNotificacion;
import com.convivir.finanzas.Gasto;
import com.convivir.finanzas.Pago;
import com.convivir.finanzas.PeriodoFacturacion;
import com.convivir.integrantes.Integrante;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropiedadHorizontal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String nit;

    @Column(nullable = false)
    private String nombre;

    private String direccion;
    private String correoContacto;
    private String telefonoContacto;

    @Enumerated(EnumType.STRING)
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;

    // PropiedadHorizontal "1" o-- "*" Inmueble : administra (agregación)
    @OneToMany(mappedBy = "propiedadHorizontal")
    private List<Inmueble> inmuebles = new ArrayList<>();

    // PropiedadHorizontal "0..1" --> "*" Inmueble : es propietaria de (asociación)
    @OneToMany(mappedBy = "propiedadPropietaria")
    private List<Inmueble> bienesComunes = new ArrayList<>();

    // PropiedadHorizontal "1" o-- "*" Integrante : agrupa (agregación)
    @OneToMany(mappedBy = "propiedadHorizontal")
    private List<Integrante> integrantes = new ArrayList<>();

    // PropiedadHorizontal "1" *-- "*" PeriodoFacturacion : factura en (composición)
    @OneToMany(mappedBy = "propiedadHorizontal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PeriodoFacturacion> periodosFacturacion = new ArrayList<>();

    // PropiedadHorizontal "1" --> "*" Pago : recibe
    @OneToMany(mappedBy = "propiedadHorizontal")
    private List<Pago> pagosRecibidos = new ArrayList<>();

    // PropiedadHorizontal "1" --> "*" Gasto : asume
    @OneToMany(mappedBy = "propiedadHorizontal")
    private List<Gasto> gastos = new ArrayList<>();

    // PropiedadHorizontal "1" *-- "*" ConfiguracionNotificacion : establece (composición)
    @OneToMany(mappedBy = "propiedadHorizontal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConfiguracionNotificacion> configuracionesNotificacion = new ArrayList<>();

    // PropiedadHorizontal "1" --> "*" Auditoria : conserva
    @OneToMany(mappedBy = "propiedadHorizontal")
    private List<Auditoria> auditorias = new ArrayList<>();

    public PropiedadHorizontal(String nit, String nombre, String direccion) {
        this.nit = nit;
        this.nombre = nombre;
        this.direccion = direccion;
    }

    public void configurarDatosGeneralesDeLaCopropiedad(String nit, String nombre, String direccion,
            String correoContacto, String telefonoContacto) {
        if (nit == null || nit.isBlank() || nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("El NIT y el nombre de la copropiedad son obligatorios.");
        }
        this.nit = nit.trim();
        this.nombre = nombre.trim();
        this.direccion = direccion;
        this.correoContacto = correoContacto;
        this.telefonoContacto = telefonoContacto;
    }
}
