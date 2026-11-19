package com.convivir.comun;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.convivir.integrantes.CuentaRepositorio;
import com.convivir.integrantes.EstadoCuenta;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Protege la API: toda ruta /api pide sesión, salvo el estado y la sesión misma,
 * y algunas solo las puede usar un rol (403). Pone en SesionActual el documento
 * de quien hace la petición, para la auditoría, y lo limpia al terminar.
 */
@Component
public class FiltroSesionActual extends OncePerRequestFilter {

    private static final List<String> RUTAS_PUBLICAS = List.of("/api/estado", "/api/sesion");

    /** Prefijo de ruta y roles (de obtenerRolDelIntegrante) que la pueden usar. */
    private static final Map<String, Set<String>> PERMISOS = Map.of(
            "/api/inmuebles", Set.of("ADMINISTRADOR"),
            "/api/tipos-inmueble", Set.of("ADMINISTRADOR"));

    private final CuentaRepositorio cuentaRepositorio;

    public FiltroSesionActual(CuentaRepositorio cuentaRepositorio) {
        this.cuentaRepositorio = cuentaRepositorio;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        try {
            String ruta = peticion.getRequestURI();
            if (ruta.startsWith("/api/") && !esPublica(ruta)) {
                String rol = SesionHttp.rol(peticion);
                if (rol == null) {
                    rechazar(respuesta, HttpServletResponse.SC_UNAUTHORIZED, "Tu sesión terminó o no has iniciado sesión. Inicia sesión de nuevo.");
                    return;
                }
                // Si la administración desactivó o bloqueó la cuenta, la sesión abierta deja de valer.
                boolean activa = cuentaRepositorio.findById(SesionHttp.cuentaId(peticion))
                        .map(c -> c.getEstado() == EstadoCuenta.ACTIVA).orElse(false);
                if (!activa) {
                    SesionHttp.cerrar(peticion);
                    rechazar(respuesta, HttpServletResponse.SC_UNAUTHORIZED, "Tu cuenta ya no está activa. Si crees que es un error, habla con la administración.");
                    return;
                }
                Set<String> permitidos = permitidosPara(ruta);
                if (permitidos != null && !permitidos.contains(rol)) {
                    rechazar(respuesta, HttpServletResponse.SC_FORBIDDEN, "Tu rol no tiene acceso a esta parte de Convivir.");
                    return;
                }
            }
            SesionActual.asignarActor(SesionHttp.documento(peticion));
            cadena.doFilter(peticion, respuesta);
        } finally {
            SesionActual.limpiar();
        }
    }

    private static boolean esPublica(String ruta) {
        return RUTAS_PUBLICAS.stream().anyMatch(p -> ruta.equals(p) || ruta.startsWith(p + "/"));
    }

    private static Set<String> permitidosPara(String ruta) {
        return PERMISOS.entrySet().stream()
                .filter(e -> ruta.equals(e.getKey()) || ruta.startsWith(e.getKey() + "/"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static void rechazar(HttpServletResponse respuesta, int estado, String mensaje) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType("application/json;charset=UTF-8");
        respuesta.getWriter().write("{\"error\":\"" + mensaje + "\"}");
    }
}
