package com.convivir.cesiones;

import java.time.LocalDate;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.comun.SesionActual;
import com.convivir.eventos.EventoSistema;
import com.convivir.eventos.PublicadorEventos;
import com.convivir.inmuebles.EstadoRegistro;
import com.convivir.inmuebles.Inmueble;
import com.convivir.integrantes.EstadoVigencia;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Un inmueble (por ejemplo un parqueadero) presta su uso a otro durante un periodo (RF-060). */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CesionUso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    private EstadoVigencia estado;

    // Inmueble "1" --> "*" CesionUso : cede
    @ManyToOne(optional = false)
    private Inmueble inmuebleQueCede;

    // Inmueble "1" --> "*" CesionUso : usa
    @ManyToOne(optional = false)
    private Inmueble inmuebleQueUsa;

    // CesionUso "*" --> "0..1" Vehiculo : ampara
    @ManyToOne
    private Vehiculo vehiculo;

    public CesionUso(LocalDate fechaInicio, LocalDate fechaFin) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public void registrarCesionDeUso(Inmueble inmuebleQueCede, Inmueble inmuebleQueUsa, Vehiculo vehiculo) {
        if (fechaInicio == null || (fechaFin != null && fechaFin.isBefore(fechaInicio))) {
            throw new ReglaDeNegocioException("La fecha final de la cesión va el mismo día de la inicial o después.");
        }
        if (inmuebleQueCede.getId().equals(inmuebleQueUsa.getId())) {
            throw new ReglaDeNegocioException("Un inmueble no se puede ceder a sí mismo.");
        }
        if (inmuebleQueCede.getEstado() != EstadoRegistro.ACTIVO || inmuebleQueUsa.getEstado() != EstadoRegistro.ACTIVO) {
            throw new ReglaDeNegocioException("Los dos inmuebles de la cesión deben estar activos.");
        }
        this.inmuebleQueCede = inmuebleQueCede;
        this.inmuebleQueUsa = inmuebleQueUsa;
        this.vehiculo = vehiculo;
        if (!validarQueElTipoDeInmueblePermitaCesion()) {
            throw new ReglaDeNegocioException("Los inmuebles de tipo " + inmuebleQueCede.getTipoInmueble().getNombre()
                    + " no se pueden ceder.");
        }
        if (!validarQueNoSeSuperpongaConOtraCesion()) {
            throw new ReglaDeNegocioException(inmuebleQueCede.getIdentificacion()
                    + " ya tiene una cesión vigente que se cruza con esas fechas.");
        }
        this.estado = EstadoVigencia.VIGENTE;
        inmuebleQueCede.getCesionesQueCede().add(this);
        inmuebleQueUsa.getCesionesQueUsa().add(this);
        publicar("CESION_REGISTRADA");
    }

    public void finalizarCesionDeUso(LocalDate fechaFin) {
        if (estado != EstadoVigencia.VIGENTE) {
            throw new ReglaDeNegocioException("La cesión ya está finalizada.");
        }
        if (fechaFin == null || fechaFin.isBefore(fechaInicio)) {
            throw new ReglaDeNegocioException("La fecha de fin va el " + fechaInicio + " o después.");
        }
        this.fechaFin = fechaFin;
        this.estado = EstadoVigencia.FINALIZADA;
        publicar("CESION_FINALIZADA");
    }

    protected boolean validarQueElTipoDeInmueblePermitaCesion() {
        return inmuebleQueCede.getTipoInmueble().isPermiteCesion();
    }

    protected boolean validarQueNoSeSuperpongaConOtraCesion() {
        LocalDate fin = fechaFin == null ? LocalDate.MAX : fechaFin;
        return inmuebleQueCede.getCesionesQueCede().stream()
                .filter(c -> c != this && c.estado == EstadoVigencia.VIGENTE)
                .noneMatch(c -> !c.fechaInicio.isAfter(fin)
                        && (c.fechaFin == null || !c.fechaFin.isBefore(fechaInicio)));
    }

    private void publicar(String tipoEvento) {
        PublicadorEventos.obtenerInstancia().publicar(new EventoSistema(tipoEvento,
                "Cesión de " + inmuebleQueCede.getIdentificacion() + " a " + inmuebleQueUsa.getIdentificacion(),
                SesionActual.documentoDelActor()));
    }
}
