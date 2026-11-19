package com.convivir.finanzas;

/** Patrón Adaptador: lo único que Convivir necesita de cualquier pasarela de pagos. */
public interface PasarelaPago {

    /** Inicia el cobro y devuelve la referencia de la transacción. */
    String iniciarCobro(double valor, String referencia);

    EstadoPago consultarResultado(String referencia);
}
