package com.convivir.integrantes;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.convivir.comun.NoAutorizadoException;
import com.convivir.comun.SesionHttp;
import com.convivir.integrantes.CuentaDatos.Credenciales;
import com.convivir.integrantes.CuentaDatos.Sesion;
import com.convivir.integrantes.CuentaDatos.SolicitudRestablecimiento;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** CU-01 Iniciar sesión, cerrar sesión (RF-004) y restablecer contraseña (RF-002). */
@RestController
@RequestMapping("/api/sesion")
public class CuentaControlador {

    private final CuentaServicio servicio;

    public CuentaControlador(CuentaServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public Sesion iniciarSesion(@Valid @RequestBody Credenciales credenciales, HttpServletRequest peticion) {
        Sesion sesion = servicio.iniciarSesion(credenciales.login(), credenciales.contrasena());
        SesionHttp.abrir(peticion, sesion);
        return sesion;
    }

    @GetMapping
    public Sesion consultarSesion(HttpServletRequest peticion) {
        Integer cuentaId = SesionHttp.cuentaId(peticion);
        if (cuentaId == null) {
            throw new NoAutorizadoException("No has iniciado sesión.");
        }
        return servicio.consultarSesion(cuentaId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrarSesion(HttpServletRequest peticion) {
        Integer cuentaId = SesionHttp.cuentaId(peticion);
        if (cuentaId != null) {
            servicio.cerrarSesion(cuentaId);
        }
        SesionHttp.cerrar(peticion);
    }

    @PostMapping("/restablecimiento")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void solicitarRestablecimiento(@Valid @RequestBody SolicitudRestablecimiento datos) {
        servicio.solicitarRestablecimiento(datos.login());
    }
}
