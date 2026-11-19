package com.convivir.finanzas;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Decisión 9: la pasarela real (PSE, tarjeta) todavía no existe. Este adaptador
 * la simula: todo cobro se aprueba, salvo los de valor terminado en 13, que se
 * rechazan, para poder probar los dos resultados.
 * Cuando haya pasarela real, solo cambia esta clase.
 */
@Component
public class AdaptadorPasarelaExterna implements PasarelaPago {

    private final Map<String, EstadoPago> cobros = new ConcurrentHashMap<>();

    @Override
    public String iniciarCobro(double valor, String referencia) {
        String transaccion = "PSE-" + referencia;
        cobros.put(transaccion, Math.round(valor) % 100 == 13 ? EstadoPago.RECHAZADO : EstadoPago.CONFIRMADO);
        return transaccion;
    }

    @Override
    public EstadoPago consultarResultado(String referencia) {
        return cobros.getOrDefault(referencia, EstadoPago.PENDIENTE);
    }
}
