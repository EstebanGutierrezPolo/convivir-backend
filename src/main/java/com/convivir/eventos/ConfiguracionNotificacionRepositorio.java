package com.convivir.eventos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import jakarta.persistence.QueryHint;

public interface ConfiguracionNotificacionRepositorio extends JpaRepository<ConfiguracionNotificacion, Integer> {

    /** Sin flush: la usa ObservadorNotificaciones en medio de una operación del dominio. */
    @QueryHints(@QueryHint(name = "org.hibernate.flushMode", value = "COMMIT"))
    Optional<ConfiguracionNotificacion> findByEvento(String evento);
}
