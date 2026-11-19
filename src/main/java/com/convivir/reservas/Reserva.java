package com.convivir.reservas;

import java.util.ArrayList;
import java.util.List;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.comun.SesionActual;
import com.convivir.eventos.EventoSistema;
import com.convivir.eventos.PublicadorEventos;
import com.convivir.finanzas.Obligacion;
import com.convivir.inmuebles.EstadoRegistro;
import com.convivir.inmuebles.Inmueble;
import com.convivir.inmuebles.TipoInmueble;
import com.convivir.integrantes.EstadoVigencia;
import com.convivir.integrantes.Integrante;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Patrón Estado: lo que la reserva puede hacer lo decide su ComportamientoReserva.
 * Patrón Estrategia: la tarifa la calcula la EstrategiaTarifa de la unidad del tipo.
 * Ninguno de los dos se guarda: se reconstruyen desde el estado y el tipo al cargar.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private double valorTarifa;
    private double valorMulta;
    private String motivoMulta;

    @Enumerated(EnumType.STRING)
    private EstadoReserva estado;

    private String motivoCancelacion;
    private String finalidad;

    @Transient
    @Getter(AccessLevel.NONE)
    private ComportamientoReserva comportamiento;

    @Transient
    @Getter(AccessLevel.NONE)
    private EstrategiaTarifa estrategiaTarifa;

    // Reserva "1" -- "1" AsignacionInmueble : ocupa el periodo de
    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(unique = true)
    private AsignacionInmueble asignacionInmueble;

    // Inmueble "1" <-- "*" Reserva : se cobra a
    @ManyToOne(optional = false)
    private Inmueble inmuebleQuePaga;

    // Integrante "1" --> "*" Reserva : realiza
    @ManyToOne(optional = false)
    private Integrante integrante;

    // Reserva "1" --> "0..*" Obligacion : genera
    @OneToMany(mappedBy = "reserva")
    private List<Obligacion> obligaciones = new ArrayList<>();

    public Reserva(AsignacionInmueble asignacionInmueble, Inmueble inmuebleQuePaga) {
        this.asignacionInmueble = asignacionInmueble;
        this.inmuebleQuePaga = inmuebleQuePaga;
    }

    /** RF-116. Queda SOLICITADA hasta que la administración la confirme. */
    public void registrarReservaDeZonaComun(Integrante integrante, String finalidad) {
        this.integrante = integrante;
        this.finalidad = finalidad;
        TipoInmueble tipo = zona().getTipoInmueble();
        if (zona().getEstado() != EstadoRegistro.ACTIVO || !tipo.isPermiteReserva()) {
            throw new ReglaDeNegocioException(zona().getIdentificacion() + " no se puede reservar (RN-37).");
        }
        if (!validarQueElIntegranteSeaResidente()) {
            throw new ReglaDeNegocioException("Solo un residente con vinculación vigente en "
                    + inmuebleQuePaga.getIdentificacion() + " puede reservar a su nombre (RN-43).");
        }
        if (!validarDisponibilidadDeLaZona()) {
            throw new ReglaDeNegocioException(zona().getIdentificacion()
                    + " ya está ocupada en ese periodo. Elige otra fecha u otra franja (RN-38).");
        }
        this.estrategiaTarifa = new FabricaEstrategiaTarifa().obtenerEstrategia(tipo.getUnidadTarifa());
        this.valorTarifa = calcularTarifaDeLaReserva();
        this.estado = EstadoReserva.SOLICITADA;
        this.comportamiento = new ReservaSolicitada();
        asignacionInmueble.vincularReserva(this);
        publicar("RESERVA_SOLICITADA");
    }

    /** RF-119. Genera la obligación ALQUILER_ZONA si la zona tiene tarifa (RN-39). */
    public void confirmarReserva() {
        comportamiento().confirmar(this);
        publicar("RESERVA_CONFIRMADA");
    }

    public void finalizarReserva() {
        comportamiento().finalizar(this);
        publicar("RESERVA_FINALIZADA");
    }

    /** RF-118. */
    public void cancelarReservaPorMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException("Escribe el motivo de la cancelación.");
        }
        comportamiento().cancelar(this, motivo.trim());
        publicar("RESERVA_CANCELADA");
    }

    /** RF-122. */
    public void aplicarMultaPorIncumplimiento(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException("Escribe el motivo de la multa.");
        }
        comportamiento().marcarIncumplida(this, motivo.trim());
        publicar("MULTA_POR_INCUMPLIMIENTO");
    }

    public double calcularTarifaDeLaReserva() {
        if (estrategiaTarifa == null) {
            estrategiaTarifa = new FabricaEstrategiaTarifa().obtenerEstrategia(zona().getTipoInmueble().getUnidadTarifa());
        }
        return estrategiaTarifa.calcular(zona().getTipoInmueble().getValorTarifa(), asignacionInmueble);
    }

    public void cambiarComportamiento(ComportamientoReserva nuevoComportamiento) {
        this.comportamiento = nuevoComportamiento;
    }

    protected boolean validarQueElIntegranteSeaResidente() {
        return integrante.getVinculaciones().stream()
                .anyMatch(v -> v.getEstado() == EstadoVigencia.VIGENTE && v.isEsResidente()
                        && v.getInmueble().getId().equals(inmuebleQuePaga.getId()));
    }

    /** Ninguna otra asignación vigente de la zona se cruza con la de esta reserva. */
    protected boolean validarDisponibilidadDeLaZona() {
        return AsignacionInmueble.consultarPeriodosOcupadosDelInmueble(zona()).stream()
                .filter(otra -> otra != asignacionInmueble)
                .noneMatch(otra -> otra.seCruzaCon(asignacionInmueble));
    }

    // ---------- Usados por los estados (mismo paquete) ----------

    void pasarA(EstadoReserva nuevoEstado, ComportamientoReserva nuevoComportamiento) {
        this.estado = nuevoEstado;
        cambiarComportamiento(nuevoComportamiento);
    }

    void registrarCancelacion(String motivo) {
        this.motivoCancelacion = motivo;
    }

    void registrarMulta(double valorMulta, String motivo) {
        this.valorMulta = valorMulta;
        this.motivoMulta = motivo;
    }

    Inmueble zona() {
        return asignacionInmueble.getInmueble();
    }

    private ComportamientoReserva comportamiento() {
        if (comportamiento == null) {
            comportamiento = switch (estado) {
                case SOLICITADA -> new ReservaSolicitada();
                case CONFIRMADA -> new ReservaConfirmada();
                default -> new ReservaCerrada();
            };
        }
        return comportamiento;
    }

    private void publicar(String tipoEvento) {
        PublicadorEventos.obtenerInstancia().publicar(new EventoSistema(tipoEvento,
                "Reserva de " + zona().getIdentificacion() + " para " + inmuebleQuePaga.getIdentificacion(),
                SesionActual.documentoDelActor()));
    }
}
