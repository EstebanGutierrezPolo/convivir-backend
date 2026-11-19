package com.convivir.inmuebles;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InmuebleRepositorio extends JpaRepository<Inmueble, Integer> {

    boolean existsByIdentificacionIgnoreCase(String identificacion);

    Optional<Inmueble> findByIdentificacionIgnoreCase(String identificacion);

    boolean existsByTipoInmuebleAndEstado(TipoInmueble tipoInmueble, EstadoRegistro estado);

    long countByTipoInmuebleAndEstado(TipoInmueble tipoInmueble, EstadoRegistro estado);

    List<Inmueble> findAllByOrderByIdentificacionAsc();
}
