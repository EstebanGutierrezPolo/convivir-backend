package com.convivir.reservas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.inmuebles.Inmueble;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Periodo en que un inmueble (por ejemplo una zona común) queda ocupado. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AsignacionInmueble {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    // Inmueble "1" --> "*" AsignacionInmueble : se asigna en
    @ManyToOne(optional = false)
    private Inmueble inmueble;

    // Reserva "1" -- "1" AsignacionInmueble : ocupa el periodo de
    @OneToOne(mappedBy = "asignacionInmueble")
    private Reserva reserva;

    // AsignacionInmueble "1" *-- "1..*" ProgramacionAsignacion : se programa en
    @OneToMany(mappedBy = "asignacionInmueble", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgramacionAsignacion> programaciones = new ArrayList<>();

    public AsignacionInmueble(LocalDate fechaInicio, LocalDate fechaFin) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin == null ? fechaInicio : fechaFin;
    }

    /** Ocupa el inmueble en el periodo, si ninguna asignación vigente se le cruza. */
    public void registrarAsignacionDelInmueble(Inmueble inmueble) {
        if (fechaInicio == null || fechaFin.isBefore(fechaInicio)) {
            throw new ReglaDeNegocioException("La fecha final va el mismo día de la inicial o después.");
        }
        if (fechaInicio.isBefore(LocalDate.now())) {
            throw new ReglaDeNegocioException("No se puede asignar un periodo que ya pasó.");
        }
        if (programaciones.isEmpty()) {
            throw new ReglaDeNegocioException("Elige al menos un día y una franja horaria.");
        }
        this.inmueble = inmueble;
        if (!validarQueFechasYHorariosNoSeSuperpongan()) {
            throw new ReglaDeNegocioException(inmueble.getIdentificacion()
                    + " ya está ocupado en ese periodo. Elige otra fecha u otra franja (RN-38).");
        }
        inmueble.getAsignaciones().add(this);
    }

    /** Asignaciones del inmueble que siguen ocupándolo: sin reserva o con reserva solicitada o confirmada. */
    public static List<AsignacionInmueble> consultarPeriodosOcupadosDelInmueble(Inmueble inmueble) {
        return inmueble.getAsignaciones().stream()
                .filter(a -> a.reserva == null || a.reserva.getEstado() == EstadoReserva.SOLICITADA
                        || a.reserva.getEstado() == EstadoReserva.CONFIRMADA)
                .toList();
    }

    protected boolean validarQueFechasYHorariosNoSeSuperpongan() {
        return consultarPeriodosOcupadosDelInmueble(inmueble).stream()
                .filter(otra -> otra != this)
                .noneMatch(this::seCruzaCon);
    }

    /** Dos asignaciones se cruzan si comparten un día de la semana dentro de sus fechas y sus horas se tocan. */
    boolean seCruzaCon(AsignacionInmueble otra) {
        LocalDate desde = fechaInicio.isAfter(otra.fechaInicio) ? fechaInicio : otra.fechaInicio;
        LocalDate hasta = fechaFin.isBefore(otra.fechaFin) ? fechaFin : otra.fechaFin;
        if (desde.isAfter(hasta)) {
            return false;
        }
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            DiaSemana diaSemana = DiaSemana.values()[dia.getDayOfWeek().ordinal()];
            for (ProgramacionAsignacion mia : programaciones) {
                for (ProgramacionAsignacion suya : otra.programaciones) {
                    if (mia.getDiaSemana() == diaSemana && suya.getDiaSemana() == diaSemana
                            && mia.getHoraInicio().isBefore(suya.getHoraFin())
                            && suya.getHoraInicio().isBefore(mia.getHoraFin())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Cuántas veces cae ese día de la semana dentro del periodo. */
    long contarDiasDe(DiaSemana diaSemana) {
        long cuenta = 0;
        for (LocalDate dia = fechaInicio; !dia.isAfter(fechaFin); dia = dia.plusDays(1)) {
            if (dia.getDayOfWeek().ordinal() == diaSemana.ordinal()) {
                cuenta++;
            }
        }
        return cuenta;
    }

    void agregarProgramacion(ProgramacionAsignacion programacion) {
        programaciones.add(programacion);
    }

    void vincularReserva(Reserva reserva) {
        this.reserva = reserva;
    }
}
