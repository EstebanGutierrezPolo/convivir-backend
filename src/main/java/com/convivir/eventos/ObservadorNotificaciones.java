package com.convivir.eventos;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/** RN-32. Si el evento tiene una configuración habilitada, se envía su notificación. */
@Component
public class ObservadorNotificaciones implements Observador {

    private final ConfiguracionNotificacionRepositorio configuracionRepositorio;
    private final NotificacionRepositorio notificacionRepositorio;

    public ObservadorNotificaciones(ConfiguracionNotificacionRepositorio configuracionRepositorio,
            NotificacionRepositorio notificacionRepositorio) {
        this.configuracionRepositorio = configuracionRepositorio;
        this.notificacionRepositorio = notificacionRepositorio;
    }

    @PostConstruct
    void suscribirse() {
        PublicadorEventos.obtenerInstancia().suscribir(this);
    }

    @PreDestroy
    void desuscribirse() {
        PublicadorEventos.obtenerInstancia().desuscribir(this);
    }

    @Override
    public void actualizar(EventoSistema evento) {
        configuracionRepositorio.findByEvento(evento.getTipoEvento())
                .filter(ConfiguracionNotificacion::isEstaHabilitada)
                .ifPresent(configuracion -> {
                    Notificacion notificacion = new Notificacion(evento.getElementoAfectado() + ": "
                            + evento.getTipoEvento().replace('_', ' ').toLowerCase());
                    notificacion.enviarNotificacionAlDestinatario(configuracion, null);
                    notificacionRepositorio.save(notificacion);
                });
    }
}
