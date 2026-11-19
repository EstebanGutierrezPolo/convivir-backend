package com.convivir.integrantes;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import jakarta.persistence.QueryHint;

public interface IntegranteRepositorio extends JpaRepository<Integrante, Integer> {

    /** Sin flush: la usa la auditoría en medio de una operación del dominio. */
    @QueryHints(@QueryHint(name = "org.hibernate.flushMode", value = "COMMIT"))
    Optional<Integrante> findByDocumento(String documento);

    boolean existsByDocumento(String documento);
}
