package com.convivir.accesos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.convivir.cesiones.Vehiculo;
import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.comun.SesionActual;
import com.convivir.eventos.EventoSistema;
import com.convivir.eventos.PublicadorEventos;
import com.convivir.inmuebles.Inmueble;
import com.convivir.integrantes.Integrante;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovimientoAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    private SentidoMovimiento sentidoMovimiento;

    private String motivoExcepcion;
    private String placaVisitante;

    // Inmueble "0..1" o-- "*" MovimientoAcceso : recibe
    @ManyToOne
    private Inmueble inmuebleDestino;

    // Inmueble "0..1" <-- "*" MovimientoAcceso : ocupa parqueadero
    @ManyToOne
    private Inmueble parqueadero;

    // AutorizacionAcceso "0..1" --> "*" MovimientoAcceso : habilita
    @ManyToOne
    private AutorizacionAcceso autorizacion;

    // Vehiculo "0..1" --> "*" MovimientoAcceso : genera
    @ManyToOne
    private Vehiculo vehiculo;

    // Integrante "1" --> "*" MovimientoAcceso : registra
    @ManyToOne(optional = false)
    private Integrante registradoPor;

    // MovimientoAcceso "0..1" --> "0..1" MovimientoAcceso : cierra (la salida cierra su ingreso)
    @OneToOne
    @JoinColumn(unique = true)
    private MovimientoAcceso ingresoQueCierra;

    public MovimientoAcceso(SentidoMovimiento sentidoMovimiento) {
        this.sentidoMovimiento = sentidoMovimiento;
    }

    /**
     * RF-033 con autorización vigente, RF-035 sin ella: entonces el motivo y el
     * inmueble de destino son obligatorios y queda en auditoría (RN-12).
     */
    public void registrarIngresoAlConjunto(Integrante registradoPor, AutorizacionAcceso autorizacion,
            Inmueble inmuebleDestino, String motivoExcepcion, String placaVisitante) {
        if (sentidoMovimiento != SentidoMovimiento.INGRESO) {
            throw new ReglaDeNegocioException("Este movimiento es una salida.");
        }
        if (autorizacion != null) {
            if (!autorizacion.validarVigenciaDeLaAutorizacion()) {
                throw new ReglaDeNegocioException("La autorización de " + autorizacion.getNombreVisitante()
                        + " no vale ahora. Regístralo como excepción con el motivo.");
            }
            this.inmuebleDestino = autorizacion.getInmueble();
            autorizacion.registrarUso();
        } else {
            if (inmuebleDestino == null) {
                throw new ReglaDeNegocioException("Indica la unidad a la que va.");
            }
            if (motivoExcepcion == null || motivoExcepcion.isBlank()) {
                throw new ReglaDeNegocioException("Escribe el motivo. Es obligatorio cuando no hay autorización previa.");
            }
            this.inmuebleDestino = inmuebleDestino;
            this.motivoExcepcion = motivoExcepcion.trim();
        }
        this.autorizacion = autorizacion;
        this.registradoPor = registradoPor;
        this.placaVisitante = Vehiculo.normalizarPlaca(placaVisitante);
        this.fechaHora = LocalDateTime.now();
        publicar(autorizacion == null ? "INGRESO_SIN_AUTORIZACION" : "INGRESO_REGISTRADO");
    }

    /** CU-10. El vehículo que entra y el parqueadero que ocupa. */
    public void asignarVehiculoYParqueadero(Vehiculo vehiculo, Inmueble parqueadero) {
        this.vehiculo = vehiculo;
        this.parqueadero = parqueadero;
        if (vehiculo != null) {
            this.placaVisitante = vehiculo.getPlaca();
        }
    }

    /** RF-034. La salida cierra un ingreso y repite su destino, vehículo y parqueadero. */
    public void registrarSalidaDelConjunto(Integrante registradoPor, MovimientoAcceso ingreso) {
        if (sentidoMovimiento != SentidoMovimiento.SALIDA) {
            throw new ReglaDeNegocioException("Este movimiento es un ingreso.");
        }
        if (ingreso == null || ingreso.sentidoMovimiento != SentidoMovimiento.INGRESO) {
            throw new ReglaDeNegocioException("Elige el ingreso de la persona que sale.");
        }
        this.ingresoQueCierra = ingreso;
        this.registradoPor = registradoPor;
        this.inmuebleDestino = ingreso.inmuebleDestino;
        this.autorizacion = ingreso.autorizacion;
        this.vehiculo = ingreso.vehiculo;
        this.parqueadero = ingreso.parqueadero;
        this.placaVisitante = ingreso.placaVisitante;
        this.fechaHora = LocalDateTime.now();
        publicar("SALIDA_REGISTRADA");
    }

    /** Ingresos que ninguna salida ha cerrado: quienes siguen dentro del conjunto. */
    public static List<MovimientoAcceso> consultarPersonasPresentesEnElConjunto(List<MovimientoAcceso> movimientos) {
        Set<Integer> cerrados = movimientos.stream()
                .filter(m -> m.ingresoQueCierra != null)
                .map(m -> m.ingresoQueCierra.getId())
                .collect(Collectors.toSet());
        return movimientos.stream()
                .filter(m -> m.sentidoMovimiento == SentidoMovimiento.INGRESO && !cerrados.contains(m.getId()))
                .toList();
    }

    private void publicar(String tipoEvento) {
        String quien = autorizacion != null ? autorizacion.getNombreVisitante() : "visitante sin autorización";
        PublicadorEventos.obtenerInstancia().publicar(new EventoSistema(tipoEvento,
                quien + " hacia " + (inmuebleDestino == null ? "" : inmuebleDestino.getIdentificacion()),
                SesionActual.documentoDelActor()));
    }
}
