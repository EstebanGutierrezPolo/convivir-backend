package com.convivir.inmuebles;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoInmuebleRepositorio extends JpaRepository<TipoInmueble, Integer> {

    boolean existsByNombreIgnoreCase(String nombre);

    List<TipoInmueble> findAllByOrderByNombreAsc();
}
