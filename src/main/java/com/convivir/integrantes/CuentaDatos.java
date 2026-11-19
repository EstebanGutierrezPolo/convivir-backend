package com.convivir.integrantes;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;

/** Lo que llega y lo que sale de la API de sesión (CU-01). */
public final class CuentaDatos {

    private CuentaDatos() {
    }

    public record Credenciales(
            @NotBlank(message = "Escribe tu usuario. Suele ser tu nombre y apellido separados por un punto.") String login,
            @NotBlank(message = "Escribe tu contraseña.") String contrasena) {
    }

    public record SolicitudRestablecimiento(
            @NotBlank(message = "Escribe tu usuario o el correo con el que te registraron.") String login) {
    }

    /** rol es el de obtenerRolDelIntegrante(): ADMINISTRADOR, VIGILANTE, PROPIETARIO, ARRENDATARIO… */
    public record Sesion(@JsonIgnore Integer cuentaId, String login, String rol, String documento, String persona,
            String cargo, List<InmuebleAccesible> inmuebles) {
    }

    /** vinculo es el de obtenerRolDelIntegranteEnInmueble(), por ejemplo ARRENDATARIO_RESIDENTE. */
    public record InmuebleAccesible(Integer id, String identificacion, String vinculo) {
    }
}
