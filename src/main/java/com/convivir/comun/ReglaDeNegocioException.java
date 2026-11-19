package com.convivir.comun;

/** Una regla de negocio no se cumple. La API responde 422 con el mensaje. */
public class ReglaDeNegocioException extends RuntimeException {
    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
