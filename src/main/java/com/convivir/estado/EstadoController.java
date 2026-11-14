package com.convivir.estado;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Punto de comprobacion del esqueleto: confirma que la API responde
 * y que alcanza la base de datos. No pertenece al modelo de dominio.
 */
@RestController
@RequestMapping("/api/estado")
public class EstadoController {

    private static final int SEGUNDOS_DE_ESPERA = 2;

    private final DataSource dataSource;

    public EstadoController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public Map<String, Object> consultarEstado() {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("aplicacion", "convivir");
        estado.put("api", "ACTIVA");
        estado.put("baseDeDatos", consultarEstadoDeLaBaseDeDatos());
        return estado;
    }

    private String consultarEstadoDeLaBaseDeDatos() {
        try (Connection conexion = dataSource.getConnection()) {
            if (!conexion.isValid(SEGUNDOS_DE_ESPERA)) {
                return "SIN_CONEXION";
            }
            return conexion.getMetaData().getDatabaseProductName() + " "
                    + conexion.getMetaData().getDatabaseProductVersion();
        } catch (SQLException e) {
            return "SIN_CONEXION";
        }
    }
}
