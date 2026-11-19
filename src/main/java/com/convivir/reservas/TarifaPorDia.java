package com.convivir.reservas;

/** Valor por cada día del periodo que tiene al menos una franja. */
public class TarifaPorDia implements EstrategiaTarifa {

    @Override
    public double calcular(double valorTarifa, AsignacionInmueble asignacionInmueble) {
        long dias = asignacionInmueble.getProgramaciones().stream()
                .map(ProgramacionAsignacion::getDiaSemana)
                .distinct()
                .mapToLong(asignacionInmueble::contarDiasDe)
                .sum();
        return Math.round(valorTarifa * dias);
    }
}
