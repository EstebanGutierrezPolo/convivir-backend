package com.convivir.finanzas;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.comun.SesionActual;
import com.convivir.eventos.EventoSistema;
import com.convivir.eventos.PublicadorEventos;
import com.convivir.inmuebles.Inmueble;
import com.convivir.reservas.Reserva;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Obligacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private ConceptoObligacion concepto;

    private String descripcion;
    private double valor;
    private double saldoPendiente;
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    private EstadoObligacion estado;

    private String motivoAnulacion;

    // Patrón Estrategia: cómo se calcula el interés de mora. No se guarda.
    @Transient
    @Getter(AccessLevel.NONE)
    private PoliticaInteresMora politicaInteres = new InteresMoraLegal();

    // Inmueble "1" *-- "*" Obligacion : adeuda
    @ManyToOne(optional = false)
    private Inmueble inmueble;

    // PeriodoFacturacion "0..1" --> "*" Obligacion : genera
    @ManyToOne
    private PeriodoFacturacion periodoFacturacion;

    // Reserva "1" --> "0..*" Obligacion : genera
    @ManyToOne
    private Reserva reserva;

    // Obligacion "1" <-- "*" Pago : abona (recorrida al revés para revertir abonos)
    @OneToMany(mappedBy = "obligacion")
    private List<Pago> pagos = new ArrayList<>();

    public Obligacion(ConceptoObligacion concepto, double valor, LocalDate fechaVencimiento) {
        if (valor <= 0) {
            throw new ReglaDeNegocioException("El valor de la obligación debe ser mayor que cero.");
        }
        if (fechaVencimiento == null) {
            throw new ReglaDeNegocioException("Indica la fecha de vencimiento.");
        }
        this.concepto = concepto;
        this.valor = valor;
        this.saldoPendiente = valor;
        this.fechaVencimiento = fechaVencimiento;
    }

    public void describir(String descripcion) {
        this.descripcion = descripcion;
    }

    /** La obligación queda a cargo del inmueble (composición: el inmueble la guarda). */
    public void cargarAlInmueble(Inmueble inmueble) {
        if (this.inmueble != null) {
            throw new ReglaDeNegocioException("La obligación ya está cargada a " + this.inmueble.getIdentificacion() + ".");
        }
        this.inmueble = inmueble;
        this.estado = EstadoObligacion.PENDIENTE;
        inmueble.getObligaciones().add(this);
        publicar("OBLIGACION_GENERADA");
    }

    /** RF-123. Un abono no puede pasar del saldo. */
    public void registrarAbonoALaObligacion(double valorAbono) {
        if (estado == EstadoObligacion.ANULADA || estado == EstadoObligacion.PAGADA) {
            throw new ReglaDeNegocioException("La obligación ya está " + estado.name().toLowerCase() + ".");
        }
        if (valorAbono <= 0) {
            throw new ReglaDeNegocioException("El valor del abono debe ser mayor que cero.");
        }
        if (valorAbono > saldoPendiente) {
            throw new ReglaDeNegocioException("El valor no puede superar el saldo pendiente de $ "
                    + Math.round(saldoPendiente) + ".");
        }
        this.saldoPendiente = aCentavos(saldoPendiente - valorAbono);
        this.estado = saldoPendiente == 0 ? EstadoObligacion.PAGADA : EstadoObligacion.PAGADA_PARCIALMENTE;
        publicar("ABONO_REGISTRADO");
    }

    /** Decisión 6, cerrada: al anular un pago, el valor vuelve al saldo. */
    public void revertirAbonoDeLaObligacion(double valorAbono) {
        this.saldoPendiente = Math.min(valor, aCentavos(saldoPendiente + valorAbono));
        if (saldoPendiente == valor) {
            this.estado = calcularDiasDeMora() > 0 ? EstadoObligacion.VENCIDA : EstadoObligacion.PENDIENTE;
        } else {
            this.estado = EstadoObligacion.PAGADA_PARCIALMENTE;
        }
        publicar("ABONO_REVERTIDO");
    }

    /** RF-069. Una obligación con pagos confirmados no se anula: primero se anulan los pagos. */
    public void anularObligacionPorMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException("Escribe el motivo de la anulación.");
        }
        if (estado == EstadoObligacion.ANULADA) {
            throw new ReglaDeNegocioException("La obligación ya está anulada.");
        }
        boolean tienePagos = pagos.stream().anyMatch(p -> p.getEstado() == EstadoPago.CONFIRMADO);
        if (tienePagos) {
            throw new ReglaDeNegocioException("La obligación tiene pagos registrados. Anula primero esos pagos.");
        }
        this.estado = EstadoObligacion.ANULADA;
        this.motivoAnulacion = motivo.trim();
        publicar("OBLIGACION_ANULADA");
    }

    public int calcularDiasDeMora() {
        if (saldoPendiente <= 0 || estado == EstadoObligacion.ANULADA || !LocalDate.now().isAfter(fechaVencimiento)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(fechaVencimiento, LocalDate.now());
    }

    /** RF-085. Informe en CSV de las obligaciones con mora entre minDias y maxDias. */
    public static byte[] generarInformeDeCartera(List<Obligacion> obligaciones, int minDias, int maxDias) {
        if (minDias > maxDias) {
            throw new ReglaDeNegocioException("El mínimo de días va antes que el máximo.");
        }
        String filas = obligaciones.stream()
                .filter(o -> o.calcularDiasDeMora() >= minDias && o.calcularDiasDeMora() <= maxDias)
                .map(o -> String.join(";", o.inmueble.getIdentificacion(), o.concepto.name(),
                        String.valueOf(Math.round(o.saldoPendiente)), String.valueOf(o.calcularDiasDeMora()),
                        String.valueOf(Math.round(o.calcularInteresDeMora()))))
                .collect(Collectors.joining("\n"));
        return ("inmueble;concepto;saldo;diasMora;interesMora\n" + filas).getBytes(StandardCharsets.UTF_8);
    }

    protected double calcularInteresDeMora() {
        return politicaInteres.calcularInteres(saldoPendiente, calcularDiasDeMora());
    }

    /** RN-22. El arrendatario responde solidariamente solo por las expensas ordinarias. */
    public boolean determinarSiHaySolidaridadDelArrendatario() {
        return concepto == ConceptoObligacion.CUOTA_ORDINARIA;
    }

    /** Con double, 0,1 + 0,2 no da 0,3 exacto: el saldo se redondea a centavos para que un pago completo deje 0. */
    private static double aCentavos(double valor) {
        return Math.round(valor * 100) / 100.0;
    }

    void asignarPeriodo(PeriodoFacturacion periodoFacturacion) {
        this.periodoFacturacion = periodoFacturacion;
    }

    void asignarReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    private void publicar(String tipoEvento) {
        PublicadorEventos.obtenerInstancia().publicar(new EventoSistema(tipoEvento,
                concepto.name() + " de " + (inmueble == null ? "" : inmueble.getIdentificacion()),
                SesionActual.documentoDelActor()));
    }
}
