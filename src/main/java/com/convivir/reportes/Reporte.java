package com.convivir.reportes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.comun.SesionActual;
import com.convivir.eventos.EventoSistema;
import com.convivir.eventos.PublicadorEventos;
import com.convivir.inmuebles.EstadoRegistro;
import com.convivir.inmuebles.Inmueble;
import com.convivir.integrantes.Empleado;
import com.convivir.integrantes.EstadoVigencia;
import com.convivir.integrantes.Integrante;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 2000)
    private String descripcion;

    /** Ruta o nombre del archivo adjunto (RF-091). Uno solo por reporte. */
    private String evidencia;

    private LocalDate fechaCreacion;

    @Enumerated(EnumType.STRING)
    private EstadoReporte estado;

    @Column(length = 2000)
    private String solucionAplicada;

    // Inmueble "0..1" *-- "*" Reporte : origina
    @ManyToOne
    private Inmueble inmueble;

    // CategoriaReporte "1" o-- "*" Reporte : clasifica
    @ManyToOne(optional = false)
    private CategoriaReporte categoria;

    // Integrante "1" --> "*" Reporte : crea
    @ManyToOne(optional = false)
    private Integrante creadoPor;

    // Integrante "0..1" --> "*" Reporte : atiende
    @ManyToOne
    private Integrante responsable;

    // Reporte "1" *-- "*" SeguimientoReporte : recibe
    @OneToMany(mappedBy = "reporte", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaHora ASC")
    private List<SeguimientoReporte> seguimientos = new ArrayList<>();

    public Reporte(String descripcion) {
        this.descripcion = descripcion == null ? null : descripcion.trim();
    }

    /** RF-090, RF-091, RN-33. */
    public void crearReporteDeNovedad(Integrante creadoPor, CategoriaReporte categoria, Inmueble inmueble,
            String evidencia) {
        if (descripcion == null || descripcion.length() < 10) {
            throw new ReglaDeNegocioException("Describe la novedad con al menos 10 caracteres: qué pasó y dónde.");
        }
        if (categoria.getEstado() != EstadoRegistro.ACTIVO) {
            throw new ReglaDeNegocioException("La categoría " + categoria.getNombre() + " está desactivada.");
        }
        if (inmueble != null && !(creadoPor instanceof Empleado)) {
            boolean vinculado = creadoPor.getVinculaciones().stream()
                    .anyMatch(v -> v.getEstado() == EstadoVigencia.VIGENTE
                            && v.getInmueble().getId().equals(inmueble.getId()));
            if (!vinculado && !inmueble.getTipoInmueble().isEsBienComun()) {
                throw new ReglaDeNegocioException("Solo puedes reportar sobre tus inmuebles o sobre zonas comunes (RN-33).");
            }
        }
        this.creadoPor = creadoPor;
        this.categoria = categoria;
        this.inmueble = inmueble;
        this.evidencia = evidencia;
        this.fechaCreacion = LocalDate.now();
        this.estado = EstadoReporte.ABIERTO;
        if (inmueble != null) {
            inmueble.getReportes().add(this);
        }
        publicar("REPORTE_CREADO");
    }

    /** RF-093. */
    public void asignarResponsableDelReporte(Integrante responsable) {
        validarQueNoEsteCerrado();
        this.responsable = responsable;
        new SeguimientoReporte("Asignado a " + responsable.obtenerNombreCompleto())
                .registrarObservacionDeSeguimiento(this);
        publicar("REPORTE_ASIGNADO");
    }

    /** RF-094. A CERRADO solo se llega con cerrarReporteConSolucion. */
    public void cambiarEstadoDelReporte(EstadoReporte nuevoEstado) {
        validarQueNoEsteCerrado();
        if (nuevoEstado == EstadoReporte.CERRADO) {
            throw new ReglaDeNegocioException("Para cerrar el reporte escribe la solución aplicada.");
        }
        if (nuevoEstado == estado) {
            throw new ReglaDeNegocioException("El reporte ya está en ese estado.");
        }
        this.estado = nuevoEstado;
        new SeguimientoReporte("Cambió a " + nuevoEstado.name()).registrarObservacionDeSeguimiento(this);
        publicar("REPORTE_CAMBIO_DE_ESTADO");
    }

    /** RF-095. */
    public void cerrarReporteConSolucion(String solucion) {
        validarQueNoEsteCerrado();
        if (solucion == null || solucion.isBlank()) {
            throw new ReglaDeNegocioException("Escribe la solución aplicada para cerrar el reporte.");
        }
        this.solucionAplicada = solucion.trim();
        this.estado = EstadoReporte.CERRADO;
        new SeguimientoReporte("Cerrado: " + solucionAplicada).registrarObservacionDeSeguimiento(this);
        publicar("REPORTE_CERRADO");
    }

    void validarQueNoEsteCerrado() {
        if (estado == EstadoReporte.CERRADO) {
            throw new ReglaDeNegocioException("El reporte ya está cerrado.");
        }
    }

    private void publicar(String tipoEvento) {
        PublicadorEventos.obtenerInstancia().publicar(new EventoSistema(tipoEvento,
                "Reporte " + (id == null ? "nuevo" : id) + " · " + categoria.getNombre(), SesionActual.documentoDelActor()));
    }
}
