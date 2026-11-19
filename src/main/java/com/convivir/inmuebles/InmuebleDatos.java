package com.convivir.inmuebles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Lo que llega y lo que sale de la API de inmuebles (CU-02). */
public final class InmuebleDatos {

    private InmuebleDatos() {
    }

    public record Solicitud(
            @NotBlank(message = "Escribe la identificación del inmueble, por ejemplo A-1203.") String identificacion,
            @NotNull(message = "Escribe el coeficiente. Sale de la escritura y define cuánto paga de cuota.") Double coeficiente,
            @NotNull(message = "Escribe el área privada en metros cuadrados.") @Positive(message = "El área debe ser mayor que cero.") Double areaM2,
            @NotNull(message = "Elige la naturaleza: bien privado, uso exclusivo o uso general.") NaturalezaInmueble naturalezaInmueble,
            @NotNull(message = "Elige el tipo de inmueble.") Integer tipoInmuebleId,
            Integer inmuebleContenedorId) {
    }

    public record Modificacion(
            @NotNull(message = "Escribe el coeficiente.") Double coeficiente,
            @NotNull(message = "Escribe el área.") @Positive(message = "El área debe ser mayor que cero.") Double areaM2,
            @NotNull(message = "Elige la naturaleza.") NaturalezaInmueble naturalezaInmueble) {
    }

    public record Motivo(@NotBlank(message = "Escribe el motivo.") String motivo) {
    }

    public record Respuesta(Integer id, String identificacion, String tipoInmueble, NaturalezaInmueble naturalezaInmueble,
            double coeficiente, double areaM2, EstadoRegistro estado, String motivoDesactivacion,
            String inmuebleContenedor, double saldoPendiente) {

        static Respuesta de(Inmueble i) {
            return new Respuesta(i.getId(), i.getIdentificacion(), i.getTipoInmueble().getNombre(),
                    i.getNaturalezaInmueble(), i.getCoeficiente(), i.getAreaM2(), i.getEstado(),
                    i.getMotivoDesactivacion(),
                    i.getInmuebleContenedor() == null ? null : i.getInmuebleContenedor().getIdentificacion(),
                    i.calcularSaldoPendienteDelInmueble());
        }
    }

    /** RF-013. Todo lo que ha pasado en el inmueble, aunque hayan cambiado sus ocupantes. */
    public record Historial(Respuesta inmueble, List<VinculacionResumen> vinculaciones,
            List<ObligacionResumen> obligaciones, List<PagoResumen> pagos, List<ReporteResumen> reportes,
            List<MovimientoResumen> movimientos) {
    }

    public record VinculacionResumen(Integer id, String integrante, String rol, boolean esResidente,
            LocalDate fechaInicio, LocalDate fechaFin, String estado, boolean autorizaAcceso) {
    }

    public record ObligacionResumen(Integer id, String concepto, String descripcion, double valor,
            double saldoPendiente, LocalDate fechaVencimiento, String estado, int diasDeMora) {
    }

    public record PagoResumen(Integer id, LocalDate fecha, double valor, String medioPago, String canalRecaudo,
            String comprobante, String estado) {
    }

    public record ReporteResumen(Integer id, LocalDate fechaCreacion, String categoria, String estado) {
    }

    public record MovimientoResumen(Integer id, LocalDateTime fechaHora, String sentido, String placa) {
    }
}
