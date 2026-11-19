package com.convivir.reservas;

import java.time.LocalTime;

import com.convivir.comun.ReglaDeNegocioException;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Día de la semana y franja horaria de una asignación. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProgramacionAsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private DiaSemana diaSemana;

    private LocalTime horaInicio;
    private LocalTime horaFin;

    // AsignacionInmueble "1" *-- "1..*" ProgramacionAsignacion : se programa en
    @ManyToOne(optional = false)
    private AsignacionInmueble asignacionInmueble;

    public ProgramacionAsignacion(DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin) {
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public void registrarProgramacionDeLaAsignacion(AsignacionInmueble asignacionInmueble) {
        if (!validarQueLaFranjaHorariaSeaValida()) {
            throw new ReglaDeNegocioException("La hora final va después de la inicial, el mismo día.");
        }
        this.asignacionInmueble = asignacionInmueble;
        asignacionInmueble.agregarProgramacion(this);
    }

    protected boolean validarQueLaFranjaHorariaSeaValida() {
        return diaSemana != null && horaInicio != null && horaFin != null && horaInicio.isBefore(horaFin);
    }
}
