package com.convivir.inmuebles;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.convivir.inmuebles.InmuebleDatos.*;

import jakarta.validation.Valid;

/** CU-02 Gestionar inmuebles. */
@RestController
@RequestMapping("/api/inmuebles")
public class InmuebleControlador {

    private final InmuebleServicio servicio;

    public InmuebleControlador(InmuebleServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Respuesta> consultarInmuebles() {
        return servicio.consultarInmuebles();
    }

    @GetMapping("/{id}/historial")
    public Historial consultarHistorial(@PathVariable Integer id) {
        return servicio.consultarHistorial(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Respuesta registrarInmueble(@Valid @RequestBody Solicitud datos) {
        return servicio.registrarInmueble(datos);
    }

    @PutMapping("/{id}")
    public Respuesta modificarInmueble(@PathVariable Integer id, @Valid @RequestBody Modificacion datos) {
        return servicio.modificarInmueble(id, datos);
    }

    @PatchMapping("/{id}/desactivar")
    public Respuesta desactivarInmueble(@PathVariable Integer id, @Valid @RequestBody Motivo datos) {
        return servicio.desactivarInmueble(id, datos.motivo());
    }

    @PatchMapping("/{id}/reactivar")
    public Respuesta reactivarInmueble(@PathVariable Integer id) {
        return servicio.reactivarInmueble(id);
    }
}
