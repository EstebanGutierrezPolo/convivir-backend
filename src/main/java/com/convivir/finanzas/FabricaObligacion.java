package com.convivir.finanzas;

import java.time.LocalDate;

import com.convivir.reservas.Reserva;

/** Patrón Fábrica: crea la obligación correcta según de dónde sale. */
public class FabricaObligacion {

    public Obligacion crearCuotaOrdinaria(double valor, LocalDate fechaVencimiento) {
        Obligacion cuota = new Obligacion(ConceptoObligacion.CUOTA_ORDINARIA, valor, fechaVencimiento);
        cuota.describir("Cuota de administración");
        return cuota;
    }

    public Obligacion crearObligacionIndividual(ConceptoObligacion concepto, double valor, LocalDate fechaVencimiento) {
        return new Obligacion(concepto, valor, fechaVencimiento);
    }

    /** RN-39. El alquiler vence el día de la reserva. Decisión 8: la fábrica lee la reserva. */
    public Obligacion crearObligacionPorReserva(Reserva reserva) {
        Obligacion alquiler = new Obligacion(ConceptoObligacion.ALQUILER_ZONA, reserva.getValorTarifa(),
                reserva.getAsignacionInmueble().getFechaInicio());
        alquiler.describir("Alquiler de " + reserva.getAsignacionInmueble().getInmueble().getIdentificacion());
        alquiler.asignarReserva(reserva);
        return alquiler;
    }
}
