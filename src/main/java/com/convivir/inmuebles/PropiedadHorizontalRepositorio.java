package com.convivir.inmuebles;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import jakarta.persistence.QueryHint;

public interface PropiedadHorizontalRepositorio extends JpaRepository<PropiedadHorizontal, Integer> {

    /**
     * Convivir administra una copropiedad por instalación. Sin flush: la usa la
     * auditoría en medio de una operación del dominio.
     */
    @QueryHints(@QueryHint(name = "org.hibernate.flushMode", value = "COMMIT"))
    Optional<PropiedadHorizontal> findFirstByOrderByIdAsc();
}
