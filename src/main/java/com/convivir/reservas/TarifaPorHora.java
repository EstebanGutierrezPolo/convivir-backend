package com.convivir.reservas;

import java.time.Duration;

/** Valor por cada hora reservada, sumando todas las franjas de todos los días. */
public class TarifaPorHora implements EstrategiaTarifa {

    @Override
    public double calcular(double valorTarifa, AsignacionInmueble asignacionInmueble) {
        double horas = asignacionInmueble.getProgramaciones().stream()
                .mapToDouble(f -> Duration.between(f.getHoraInicio(), f.getHoraFin()).toMinutes() / 60.0
                        * asignacionInmueble.contarDiasDe(f.getDiaSemana()))
                .sum();
        return Math.round(valorTarifa * horas);
    }
}
