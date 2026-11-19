package com.convivir.integrantes;

import com.convivir.comun.ReglaDeNegocioException;

/**
 * Patrón Fábrica: crea el tipo de integrante que corresponde al rol elegido.
 * datoDelRol es lo propio de cada tipo: contrato, parentesco, cargo en el
 * consejo o cargo del empleado.
 */
public class FabricaIntegrante {

    public Integrante crearIntegrante(String rol, String documento, String nombres, String apellidos,
            String datoDelRol) {
        if (rol == null) {
            throw new ReglaDeNegocioException("Elige el rol de la persona.");
        }
        return switch (rol) {
            case "PROPIETARIO" -> new Propietario(documento, nombres, apellidos);
            case "ARRENDATARIO" -> new Arrendatario(documento, nombres, apellidos,
                    exigir(datoDelRol, "Escribe el número del contrato de arrendamiento."));
            case "RESIDENTE_AUTORIZADO" -> new ResidenteAutorizado(documento, nombres, apellidos,
                    exigir(datoDelRol, "Escribe el parentesco con el propietario o arrendatario."));
            case "MIEMBRO_CONSEJO" -> new MiembroConsejo(documento, nombres, apellidos,
                    exigir(datoDelRol, "Escribe el cargo en el consejo."));
            case "EMPLEADO" -> new Empleado(documento, nombres, apellidos, cargo(datoDelRol));
            default -> throw new ReglaDeNegocioException("El rol " + rol + " no existe. Usa PROPIETARIO, ARRENDATARIO, "
                    + "RESIDENTE_AUTORIZADO, MIEMBRO_CONSEJO o EMPLEADO.");
        };
    }

    private String exigir(String dato, String mensaje) {
        if (dato == null || dato.isBlank()) {
            throw new ReglaDeNegocioException(mensaje);
        }
        return dato.trim();
    }

    private CargoEmpleado cargo(String dato) {
        try {
            return CargoEmpleado.valueOf(exigir(dato, "Elige el cargo del empleado: ADMINISTRADOR o VIGILANTE."));
        } catch (IllegalArgumentException e) {
            throw new ReglaDeNegocioException("El cargo del empleado es ADMINISTRADOR o VIGILANTE.");
        }
    }
}
