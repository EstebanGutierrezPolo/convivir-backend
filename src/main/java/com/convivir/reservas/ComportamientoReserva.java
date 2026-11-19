package com.convivir.reservas;

/** Patrón Estado: cada estado de la reserva responde a las mismas acciones a su manera. */
public interface ComportamientoReserva {

    void confirmar(Reserva reserva);

    void cancelar(Reserva reserva, String motivo);

    void finalizar(Reserva reserva);

    void marcarIncumplida(Reserva reserva, String motivo);
}
