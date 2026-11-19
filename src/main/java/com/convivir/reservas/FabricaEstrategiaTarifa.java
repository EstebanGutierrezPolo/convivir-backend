package com.convivir.reservas;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.inmuebles.UnidadTarifa;

/** Patrón Fábrica: entrega la estrategia que corresponde a la unidad de tarifa. */
public class FabricaEstrategiaTarifa {

    public EstrategiaTarifa obtenerEstrategia(UnidadTarifa unidadTarifa) {
        if (unidadTarifa == null) {
            throw new ReglaDeNegocioException("El tipo de la zona no tiene unidad de tarifa configurada.");
        }
        return switch (unidadTarifa) {
            case POR_HORA -> new TarifaPorHora();
            case POR_DIA -> new TarifaPorDia();
            case POR_EVENTO -> new TarifaPorEvento();
        };
    }
}
