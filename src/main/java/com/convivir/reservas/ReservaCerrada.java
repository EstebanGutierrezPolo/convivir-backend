package com.convivir.reservas;

import com.convivir.comun.ReglaDeNegocioException;

/** CANCELADA, FINALIZADA o INCUMPLIDA: ya no admite cambios. */
public class ReservaCerrada implements ComportamientoReserva {

    private static final String MENSAJE = "La reserva ya está cerrada y no admite cambios.";

    @Override
    public void confirmar(Reserva reserva) {
        throw new ReglaDeNegocioException(MENSAJE);
    }

    @Override
    public void cancelar(Reserva reserva, String motivo) {
        throw new ReglaDeNegocioException(MENSAJE);
    }

    @Override
    public void finalizar(Reserva reserva) {
        throw new ReglaDeNegocioException(MENSAJE);
    }

    @Override
    public void marcarIncumplida(Reserva reserva, String motivo) {
        throw new ReglaDeNegocioException(MENSAJE);
    }
}
