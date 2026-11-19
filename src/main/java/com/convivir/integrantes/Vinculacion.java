package com.convivir.integrantes;

import java.time.LocalDate;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.comun.SesionActual;
import com.convivir.eventos.EventoSistema;
import com.convivir.eventos.PublicadorEventos;
import com.convivir.inmuebles.EstadoRegistro;
import com.convivir.inmuebles.Inmueble;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Clase intermedia de Integrante "*" -- "*" Inmueble. No es @ManyToMany porque
 * guarda datos propios: fechas, estado y si autoriza accesos.
 * Decisión 7: el tipo de vínculo se deduce del tipo de integrante y de esResidente.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vinculacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private boolean esResidente;
    private double porcentajePropiedad;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    private EstadoVigencia estado;

    private boolean autorizaAcceso;
    private String motivoCorreccion;

    // Integrante "1" --> "*" Vinculacion : tiene
    @ManyToOne(optional = false)
    private Integrante integrante;

    // Vinculacion "*" --> "1" Inmueble : registra
    @ManyToOne(optional = false)
    private Inmueble inmueble;

    public Vinculacion(boolean esResidente, double porcentajePropiedad, LocalDate fechaInicio) {
        this.esResidente = esResidente;
        this.porcentajePropiedad = porcentajePropiedad;
        this.fechaInicio = fechaInicio == null ? LocalDate.now() : fechaInicio;
    }

    /** RF-020, RN-03. */
    public void vincularIntegranteAlInmueble(Integrante integrante, Inmueble inmueble) {
        if (inmueble.getEstado() != EstadoRegistro.ACTIVO) {
            throw new ReglaDeNegocioException("El inmueble " + inmueble.getIdentificacion() + " está desactivado.");
        }
        if (integrante.getEstado() != EstadoRegistro.ACTIVO) {
            throw new ReglaDeNegocioException(integrante.obtenerNombreCompleto() + " está desactivado.");
        }
        if (integrante instanceof Empleado) {
            throw new ReglaDeNegocioException("Los empleados no se vinculan a un inmueble.");
        }
        boolean esPropietario = integrante instanceof Propietario;
        if (esPropietario) {
            inmueble.getVinculaciones().stream()
                    .filter(v -> v.estado == EstadoVigencia.VIGENTE && v.integrante instanceof Propietario)
                    .findFirst()
                    .ifPresent(v -> {
                        throw new ReglaDeNegocioException("El inmueble " + inmueble.getIdentificacion()
                                + " ya tiene una propiedad vigente de " + v.integrante.obtenerNombreCompleto()
                                + ". Finalízala antes de registrar al nuevo propietario.");
                    });
            if (porcentajePropiedad <= 0 || porcentajePropiedad > 100) {
                throw new ReglaDeNegocioException("El porcentaje de propiedad va entre 1 y 100.");
            }
        } else {
            this.porcentajePropiedad = 0;
        }
        boolean yaVinculado = inmueble.getVinculaciones().stream()
                .anyMatch(v -> v.estado == EstadoVigencia.VIGENTE && v.integrante.getId().equals(integrante.getId()));
        if (yaVinculado) {
            throw new ReglaDeNegocioException(integrante.obtenerNombreCompleto() + " ya está vinculado a "
                    + inmueble.getIdentificacion() + ".");
        }
        this.integrante = integrante;
        this.inmueble = inmueble;
        this.estado = EstadoVigencia.VIGENTE;
        this.autorizaAcceso = esPropietario || integrante instanceof Arrendatario;
        inmueble.getVinculaciones().add(this);
        integrante.getVinculaciones().add(this);
        publicar("VINCULACION_REGISTRADA");
    }

    /** RF-022. La vinculación no se borra: queda en el historial del inmueble. */
    public void finalizarVinculacion(LocalDate fechaFin) {
        if (estado != EstadoVigencia.VIGENTE) {
            throw new ReglaDeNegocioException("La vinculación ya está finalizada.");
        }
        if (fechaFin == null || fechaFin.isBefore(fechaInicio)) {
            throw new ReglaDeNegocioException("La fecha de fin va el " + fechaInicio + " o después.");
        }
        this.fechaFin = fechaFin;
        this.estado = EstadoVigencia.FINALIZADA;
        this.autorizaAcceso = false;
        publicar("VINCULACION_FINALIZADA");
    }

    /** RF-023. Toda corrección deja su motivo. */
    public void corregirDatosDeLaVinculacion(boolean esResidente, double porcentajePropiedad, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException("Escribe el motivo de la corrección.");
        }
        this.esResidente = esResidente;
        this.porcentajePropiedad = integrante instanceof Propietario ? porcentajePropiedad : 0;
        this.motivoCorreccion = motivo.trim();
        publicar("VINCULACION_CORREGIDA");
    }

    /** RF-024, RN-08. */
    public void asignarPermisoParaAutorizarAccesos(boolean autoriza) {
        if (estado != EstadoVigencia.VIGENTE) {
            throw new ReglaDeNegocioException("Solo una vinculación vigente puede autorizar accesos.");
        }
        this.autorizaAcceso = autoriza;
        publicar(autoriza ? "PERMISO_DE_ACCESO_ASIGNADO" : "PERMISO_DE_ACCESO_RETIRADO");
    }

    private void publicar(String tipoEvento) {
        PublicadorEventos.obtenerInstancia().publicar(new EventoSistema(tipoEvento,
                "Vinculación de " + (integrante == null ? "" : integrante.getDocumento()) + " con "
                        + (inmueble == null ? "" : inmueble.getIdentificacion()),
                SesionActual.documentoDelActor()));
    }
}
