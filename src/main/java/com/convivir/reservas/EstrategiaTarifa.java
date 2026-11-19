package com.convivir.reservas;

/** Patrón Estrategia: cómo se cobra una reserva según la unidad de tarifa del tipo. */
public interface EstrategiaTarifa {
    double calcular(double valorTarifa, AsignacionInmueble asignacionInmueble);
}
