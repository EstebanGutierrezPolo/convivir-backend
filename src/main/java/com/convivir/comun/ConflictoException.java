package com.convivir.comun;

/** Choca con lo que ya existe: un dato único repetido o algo que depende de él. La API responde 409. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
