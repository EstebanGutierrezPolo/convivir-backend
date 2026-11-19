package com.convivir.integrantes;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaRepositorio extends JpaRepository<Cuenta, Integer> {

    Optional<Cuenta> findByLogin(String login);

    boolean existsByLogin(String login);
}
