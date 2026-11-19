package com.convivir.inmuebles;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Lo que llega y lo que sale de la API de tipos de inmueble (CU-19). */
public final class TipoInmuebleDatos {

    private TipoInmuebleDatos() {
    }

    public record Solicitud(
            @NotBlank(message = "Escribe el nombre del tipo de inmueble.") String nombre,
            String descripcion,
            boolean esBienComun,
            boolean permiteCesion,
            boolean permiteReserva,
            boolean generaCuota,
            @PositiveOrZero(message = "La tarifa no puede ser negativa.") double valorTarifa,
            UnidadTarifa unidadTarifa,
            @PositiveOrZero(message = "La multa no puede ser negativa.") double valorMulta) {
    }

    public record Respuesta(Integer id, String nombre, String descripcion, boolean esBienComun, boolean permiteCesion,
            boolean permiteReserva, boolean generaCuota, double valorTarifa, UnidadTarifa unidadTarifa,
            double valorMulta, EstadoRegistro estado) {

        static Respuesta de(TipoInmueble t) {
            return new Respuesta(t.getId(), t.getNombre(), t.getDescripcion(), t.isEsBienComun(), t.isPermiteCesion(),
                    t.isPermiteReserva(), t.isGeneraCuota(), t.getValorTarifa(), t.getUnidadTarifa(), t.getValorMulta(),
                    t.getEstado());
        }
    }
}
