package com.convivir.eventos;

import java.time.LocalDateTime;

import lombok.Getter;

/** Lo que una clase del dominio avisa que pasó. No se guarda: lo reciben los observadores. */
@Getter
public class EventoSistema {

    private final String tipoEvento;
    private final String elementoAfectado;
    private final LocalDateTime fechaHora;
    private final String documentoActor;

    public EventoSistema(String tipoEvento, String elementoAfectado, String documentoActor) {
        this.tipoEvento = tipoEvento;
        this.elementoAfectado = elementoAfectado;
        this.documentoActor = documentoActor;
        this.fechaHora = LocalDateTime.now();
    }
}
