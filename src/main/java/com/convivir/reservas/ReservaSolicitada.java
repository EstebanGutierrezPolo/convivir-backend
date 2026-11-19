package com.convivir.reservas;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.finanzas.FabricaObligacion;
import com.convivir.finanzas.Obligacion;

/** SOLICITADA: se puede confirmar o cancelar. */
public class ReservaSolicitada implements ComportamientoReserva {

    @Override
    public void confirmar(Reserva reserva) {
        if (!reserva.validarDisponibilidadDeLaZona()) {
            throw new ReglaDeNegocioException("No se puede confirmar: otra reserva confirmada ocupa "
                    + reserva.zona().getIdentificacion() + " en ese periodo (RN-38).");
        }
        reserva.pasarA(EstadoReserva.CONFIRMADA, new ReservaConfirmada());
        if (reserva.getValorTarifa() > 0) {
            Obligacion alquiler = new FabricaObligacion().crearObligacionPorReserva(reserva);
            alquiler.cargarAlInmueble(reserva.getInmuebleQuePaga());
            reserva.getObligaciones().add(alquiler);
        }
    }

    @Override
    public void cancelar(Reserva reserva, String motivo) {
        reserva.registrarCancelacion(motivo);
        reserva.pasarA(EstadoReserva.CANCELADA, new ReservaCerrada());
    }

    @Override
    public void finalizar(Reserva reserva) {
        throw new ReglaDeNegocioException("La reserva todavía no está confirmada: confírmala o cancélala.");
    }

    @Override
    public void marcarIncumplida(Reserva reserva, String motivo) {
        throw new ReglaDeNegocioException("Solo se multa una reserva confirmada.");
    }
}
