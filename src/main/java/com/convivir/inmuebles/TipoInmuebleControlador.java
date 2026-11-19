package com.convivir.inmuebles;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/** CU-19 Gestionar tipos de inmueble. */
@RestController
@RequestMapping("/api/tipos-inmueble")
public class TipoInmuebleControlador {

    private final TipoInmuebleServicio servicio;

    public TipoInmuebleControlador(TipoInmuebleServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<TipoInmuebleDatos.Respuesta> consultarTipos() {
        return servicio.consultarTipos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TipoInmuebleDatos.Respuesta registrarTipo(@Valid @RequestBody TipoInmuebleDatos.Solicitud datos) {
        return servicio.registrarTipo(datos);
    }

    @PutMapping("/{id}")
    public TipoInmuebleDatos.Respuesta modificarTipo(@PathVariable Integer id,
            @Valid @RequestBody TipoInmuebleDatos.Solicitud datos) {
        return servicio.modificarTipo(id, datos);
    }

    @PatchMapping("/{id}/desactivar")
    public TipoInmuebleDatos.Respuesta desactivarTipo(@PathVariable Integer id) {
        return servicio.desactivarTipo(id);
    }
}
