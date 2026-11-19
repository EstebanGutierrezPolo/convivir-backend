package com.convivir.reservas;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.finanzas.ConceptoObligacion;
import com.convivir.finanzas.EstadoObligacion;
import com.convivir.finanzas.FabricaObligacion;
import com.convivir.finanzas.Obligacion;

/** CONFIRMADA: se puede cancelar, finalizar o multar por incumplimiento. */
public class ReservaConfirmada implements ComportamientoReserva {

    @Override
    public void confirmar(Reserva reserva) {
        throw new ReglaDeNegocioException("La reserva ya está confirmada.");
    }

    /** El cobro del alquiler que siga pendiente se anula con la reserva. */
    @Override
    public void cancelar(Reserva reserva, String motivo) {
        reserva.getObligaciones().stream()
                .filter(o -> o.getEstado() == EstadoObligacion.PENDIENTE)
                .forEach(o -> o.anularObligacionPorMotivo("Reserva cancelada: " + motivo));
        reserva.registrarCancelacion(motivo);
        reserva.pasarA(EstadoReserva.CANCELADA, new ReservaCerrada());
    }

    @Override
    public void finalizar(Reserva reserva) {
        reserva.pasarA(EstadoReserva.FINALIZADA, new ReservaCerrada());
    }

    /** La multa es la del tipo de la zona y se cobra al inmueble que paga. */
    @Override
    public void marcarIncumplida(Reserva reserva, String motivo) {
        double multa = reserva.zona().getTipoInmueble().getValorMulta();
        reserva.registrarMulta(multa, motivo);
        reserva.pasarA(EstadoReserva.INCUMPLIDA, new ReservaCerrada());
        if (multa > 0) {
            Obligacion obligacion = new FabricaObligacion().crearObligacionIndividual(ConceptoObligacion.MULTA, multa,
                    reserva.getAsignacionInmueble().getFechaFin().plusDays(15));
            obligacion.describir("Multa por incumplimiento en " + reserva.zona().getIdentificacion() + ": " + motivo);
            obligacion.cargarAlInmueble(reserva.getInmuebleQuePaga());
            reserva.getObligaciones().add(obligacion);
        }
    }
}
