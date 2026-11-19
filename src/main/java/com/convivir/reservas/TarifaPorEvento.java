package com.convivir.reservas;

/** Un solo valor por la reserva, sin importar cuánto dure. */
public class TarifaPorEvento implements EstrategiaTarifa {

    @Override
    public double calcular(double valorTarifa, AsignacionInmueble asignacionInmueble) {
        return valorTarifa;
    }
}
