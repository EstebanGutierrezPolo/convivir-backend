package com.convivir.finanzas;

import java.time.LocalDate;
import java.util.UUID;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.comun.SesionActual;
import com.convivir.eventos.EventoSistema;
import com.convivir.eventos.PublicadorEventos;
import com.convivir.inmuebles.Inmueble;
import com.convivir.inmuebles.PropiedadHorizontal;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDate fecha;
    private double valor;

    @Enumerated(EnumType.STRING)
    private MedioPago medioPago;

    @Enumerated(EnumType.STRING)
    private CanalRecaudo canalRecaudo;

    @Column(unique = true)
    private String comprobante;

    /** RN-24. Nombre y documento de quien entregó el dinero. */
    private String pagador;

    @Getter(AccessLevel.NONE)
    private String referenciaPasarela;

    // Patrón Adaptador: el servicio de pagos externo detrás de una interfaz. No se guarda.
    @Transient
    @Getter(AccessLevel.NONE)
    private PasarelaPago pasarela;

    @Enumerated(EnumType.STRING)
    private EstadoPago estado;

    private String motivoAnulacion;

    // Obligacion "1" <-- "*" Pago : abona
    @ManyToOne(optional = false)
    private Obligacion obligacion;

    // Inmueble "1" *-- "*" Pago : realiza
    @ManyToOne(optional = false)
    private Inmueble inmueble;

    // PropiedadHorizontal "1" --> "*" Pago : recibe
    @ManyToOne
    private PropiedadHorizontal propiedadHorizontal;

    public Pago(double valor, MedioPago medioPago, CanalRecaudo canalRecaudo) {
        this.valor = valor;
        this.medioPago = medioPago;
        this.canalRecaudo = canalRecaudo;
        this.fecha = LocalDate.now();
    }

    public void asignarPasarela(PasarelaPago pasarela) {
        this.pasarela = pasarela;
    }

    /** RN-24. Fecha del pago y quién lo entregó, en la oficina. */
    public void registrarDatosDelPagoPresencial(LocalDate fecha, String pagador) {
        if (fecha == null || fecha.isAfter(LocalDate.now())) {
            throw new ReglaDeNegocioException("La fecha del pago no puede ser futura.");
        }
        if (pagador == null || pagador.isBlank()) {
            throw new ReglaDeNegocioException("Escribe quién entregó el pago.");
        }
        this.fecha = fecha;
        this.pagador = pagador.trim();
    }

    /**
     * RF-074 en línea: queda PENDIENTE hasta que la pasarela responda.
     * RF-079 presencial: se confirma, abona la obligación y genera el recibo.
     */
    public void procesarPagoDeLaObligacion(Obligacion obligacion) {
        if (valor <= 0) {
            throw new ReglaDeNegocioException("El valor del pago debe ser mayor que cero.");
        }
        if (valor > obligacion.getSaldoPendiente()) {
            throw new ReglaDeNegocioException("El valor no puede superar el saldo pendiente de $ "
                    + Math.round(obligacion.getSaldoPendiente()) + ".");
        }
        boolean enLinea = canalRecaudo == CanalRecaudo.EN_LINEA;
        if (enLinea && pasarela == null) {
            throw new ReglaDeNegocioException("No hay pasarela de pagos disponible en este momento.");
        }
        if (!enLinea && medioPago == MedioPago.PASARELA) {
            throw new ReglaDeNegocioException("Un pago presencial es en efectivo, transferencia o cheque.");
        }
        // Validado todo, el pago queda ligado a su obligación, su inmueble y la copropiedad.
        this.obligacion = obligacion;
        this.inmueble = obligacion.getInmueble();
        this.propiedadHorizontal = inmueble.getPropiedadHorizontal();
        inmueble.getPagos().add(this);
        obligacion.getPagos().add(this);
        if (enLinea) {
            this.referenciaPasarela = pasarela.iniciarCobro(valor, "OBL-" + obligacion.getId() + "-" + UUID.randomUUID());
            this.estado = EstadoPago.PENDIENTE;
            return;
        }
        confirmar();
    }

    /**
     * RF-076, RN-23. El pago en línea solo abona cuando la pasarela lo confirma.
     * La pasarela no se guarda: después de cargar el pago, el servicio la asigna
     * de nuevo con asignarPasarela() antes de llamar este método.
     */
    public boolean validarResultadoDeLaPasarela() {
        if (estado != EstadoPago.PENDIENTE) {
            return estado == EstadoPago.CONFIRMADO;
        }
        if (pasarela == null) {
            throw new IllegalStateException("Asigna la pasarela con asignarPasarela() antes de consultar el resultado.");
        }
        EstadoPago resultado = pasarela.consultarResultado(referenciaPasarela);
        if (resultado == EstadoPago.CONFIRMADO) {
            confirmar();
            return true;
        }
        if (resultado == EstadoPago.RECHAZADO) {
            this.estado = EstadoPago.RECHAZADO;
            publicar("PAGO_RECHAZADO");
        }
        return false;
    }

    /** RF-077, RF-080. Número de comprobante o recibo de caja. */
    public String generarComprobanteDePago() {
        if (estado != EstadoPago.CONFIRMADO) {
            throw new ReglaDeNegocioException("Solo un pago confirmado tiene comprobante.");
        }
        if (comprobante == null) {
            String prefijo = canalRecaudo == CanalRecaudo.PRESENCIAL ? "RC-" : "CP-";
            comprobante = prefijo + fecha.getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        return comprobante;
    }

    /** RF-081. Decisión 6: el valor vuelve al saldo de la obligación. */
    public void anularPagoPorMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException("Escribe el motivo de la anulación. Queda en auditoría.");
        }
        if (estado != EstadoPago.CONFIRMADO) {
            throw new ReglaDeNegocioException("Solo se anula un pago confirmado.");
        }
        this.estado = EstadoPago.ANULADO;
        this.motivoAnulacion = motivo.trim();
        obligacion.revertirAbonoDeLaObligacion(valor);
        publicar("PAGO_ANULADO");
    }

    private void confirmar() {
        this.estado = EstadoPago.CONFIRMADO;
        obligacion.registrarAbonoALaObligacion(valor);
        generarComprobanteDePago();
        publicar("PAGO_REGISTRADO");
    }

    private void publicar(String tipoEvento) {
        PublicadorEventos.obtenerInstancia().publicar(new EventoSistema(tipoEvento,
                "Pago de $ " + Math.round(valor) + " a " + inmueble.getIdentificacion(), SesionActual.documentoDelActor()));
    }
}
