package com.convivir.integrantes;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.convivir.comun.NoAutorizadoException;
import com.convivir.comun.SesionActual;
import com.convivir.integrantes.CuentaDatos.InmuebleAccesible;
import com.convivir.integrantes.CuentaDatos.Sesion;

import lombok.extern.slf4j.Slf4j;

/**
 * CU-01, siguiendo su diagrama de secuencia. Las reglas (intentos, bloqueo,
 * contraseña) están en Cuenta; el servicio busca, llama y guarda.
 * Los mensajes de rechazo no dicen si el usuario existe, para no revelar cuentas.
 */
@Slf4j
@Service
@Transactional
public class CuentaServicio {

    static final String CREDENCIALES_INVALIDAS = "El usuario o la contraseña no coinciden. Revisa e inténtalo de nuevo; "
            + "después de " + Cuenta.MAXIMO_INTENTOS + " intentos fallidos la cuenta se bloquea.";
    static final String CUENTA_BLOQUEADA = "Tu cuenta está bloqueada por intentos fallidos. Restablece tu contraseña "
            + "o pide a la administración que la desbloquee.";

    private final CuentaRepositorio cuentaRepositorio;

    public CuentaServicio(CuentaRepositorio cuentaRepositorio) {
        this.cuentaRepositorio = cuentaRepositorio;
    }

    /**
     * RF-001. Devuelve la sesión o lanza 401. El 401 no revierte la transacción:
     * el intento fallido y el bloqueo (RNF-01) tienen que quedar guardados.
     */
    @Transactional(noRollbackFor = NoAutorizadoException.class)
    public Sesion iniciarSesion(String login, String contrasena) {
        Cuenta cuenta = cuentaRepositorio.findByLogin(login.trim().toLowerCase())
                .orElseThrow(() -> new NoAutorizadoException(CREDENCIALES_INVALIDAS));
        if (cuenta.getEstado() == EstadoCuenta.BLOQUEADA) {
            throw new NoAutorizadoException(CUENTA_BLOQUEADA);
        }
        if (cuenta.getEstado() != EstadoCuenta.ACTIVA) {
            throw new NoAutorizadoException(CREDENCIALES_INVALIDAS);
        }
        // El evento INICIO_SESION debe quedar a nombre de quien entra.
        SesionActual.asignarActor(cuenta.getIntegrante().getDocumento());
        boolean entro = cuenta.iniciarSesionConLoginYContrasena(contrasena);
        cuentaRepositorio.save(cuenta);
        if (!entro) {
            throw new NoAutorizadoException(cuenta.getEstado() == EstadoCuenta.BLOQUEADA ? CUENTA_BLOQUEADA : CREDENCIALES_INVALIDAS);
        }
        return sesionDe(cuenta);
    }

    /** La sesión vigente, para recuperarla al recargar la página. */
    @Transactional(readOnly = true)
    public Sesion consultarSesion(Integer cuentaId) {
        Cuenta cuenta = cuentaRepositorio.findById(cuentaId)
                .filter(c -> c.getEstado() == EstadoCuenta.ACTIVA)
                .orElseThrow(() -> new NoAutorizadoException("Tu sesión terminó. Inicia sesión de nuevo."));
        return sesionDe(cuenta);
    }

    /** RF-004. */
    public void cerrarSesion(Integer cuentaId) {
        cuentaRepositorio.findById(cuentaId).ifPresent(Cuenta::cerrarSesionActiva);
    }

    /**
     * RF-002. El servicio de notificaciones está simulado (decisión 9): el envío
     * del enlace queda en el registro del backend. Responde igual exista o no la
     * cuenta, para no revelar cuáles existen.
     */
    @Transactional(readOnly = true)
    public void solicitarRestablecimiento(String login) {
        cuentaRepositorio.findByLogin(login.trim().toLowerCase())
                .ifPresent(c -> log.info("Enlace de restablecimiento enviado a la cuenta {} (envío simulado)", c.getLogin()));
    }

    private Sesion sesionDe(Cuenta cuenta) {
        Integrante integrante = cuenta.getIntegrante();
        List<InmuebleAccesible> inmuebles = integrante instanceof Empleado ? List.of()
                : cuenta.obtenerInmueblesAccesibles().stream()
                        .map(i -> new InmuebleAccesible(i.getId(), i.getIdentificacion(), cuenta.obtenerRolDelIntegranteEnInmueble(i)))
                        .toList();
        return new Sesion(cuenta.getId(), cuenta.getLogin(), cuenta.obtenerRolDelIntegrante(), integrante.getDocumento(),
                integrante.obtenerNombreCompleto(), cargo(integrante), inmuebles);
    }

    private String cargo(Integrante integrante) {
        if (integrante instanceof Empleado e) {
            return e.getCargoEmpleado() == CargoEmpleado.ADMINISTRADOR ? "Administración"
                    : "Vigilante" + (e.getTurno() == null ? "" : " · turno " + e.getTurno());
        }
        if (integrante instanceof MiembroConsejo m) {
            return "Consejo · " + m.getCargoEnConsejo();
        }
        return switch (integrante.obtenerRolDelIntegrante()) {
            case "PROPIETARIO" -> "Propietario";
            case "ARRENDATARIO" -> "Arrendatario";
            default -> "Residente autorizado";
        };
    }
}
