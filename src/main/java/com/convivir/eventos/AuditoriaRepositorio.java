package com.convivir.eventos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepositorio extends JpaRepository<Auditoria, Integer> {

    List<Auditoria> findTop100ByOrderByFechaHoraDesc();
}
