package com.convivir.eventos;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Patrón Observador (sujeto) con una sola instancia (Singleton), para que las
 * clases del dominio publiquen sin recibirlo por parámetro: es la dependencia
 * "Inmueble ..> PublicadorEventos : publica" del diagrama.
 * Los observadores se avisan en la misma transacción: si la operación falla,
 * tampoco queda la auditoría.
 */
public final class PublicadorEventos {

    private static final PublicadorEventos INSTANCIA = new PublicadorEventos();

    private final List<Observador> observadores = new CopyOnWriteArrayList<>();

    private PublicadorEventos() {
    }

    public static PublicadorEventos obtenerInstancia() {
        return INSTANCIA;
    }

    public void suscribir(Observador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    public void desuscribir(Observador observador) {
        observadores.remove(observador);
    }

    public void publicar(EventoSistema evento) {
        for (Observador observador : observadores) {
            observador.actualizar(evento);
        }
    }
}
