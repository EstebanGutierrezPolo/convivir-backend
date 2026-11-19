package com.convivir.comun;

/** No hay sesión o las credenciales no sirven. La API responde 401 con el mensaje. */
public class NoAutorizadoException extends RuntimeException {
    public NoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
