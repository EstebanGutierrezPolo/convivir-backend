package com.convivir.comun;

/** El registro pedido no existe. La API responde 404 con el mensaje. */
public class NoEncontradoException extends RuntimeException {
    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
