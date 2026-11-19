package com.convivir.eventos;

/** Patrón Observador: quien quiera enterarse de los eventos del dominio. */
public interface Observador {
    void actualizar(EventoSistema evento);
}
