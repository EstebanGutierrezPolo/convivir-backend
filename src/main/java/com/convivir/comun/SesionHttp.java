package com.convivir.comun;

import com.convivir.integrantes.CuentaDatos.Sesion;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Guarda en la sesión del servidor (cookie JSESSIONID) quién entró. Solo se
 * guardan el id de la cuenta, el documento y el rol; nunca la contraseña.
 */
public final class SesionHttp {

    private static final String CUENTA = "convivir.cuentaId";
    private static final String DOCUMENTO = "convivir.documento";
    private static final String ROL = "convivir.rol";
    private static final int MINUTOS_SIN_USO = 30;

    private SesionHttp() {
    }

    /** Renueva el identificador de sesión al entrar, para evitar que alguien fije uno de antemano. */
    public static void abrir(HttpServletRequest peticion, Sesion sesion) {
        HttpSession anterior = peticion.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        HttpSession nueva = peticion.getSession(true);
        nueva.setMaxInactiveInterval(MINUTOS_SIN_USO * 60);
        nueva.setAttribute(CUENTA, sesion.cuentaId());
        nueva.setAttribute(DOCUMENTO, sesion.documento());
        nueva.setAttribute(ROL, sesion.rol());
    }

    public static void cerrar(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
    }

    public static Integer cuentaId(HttpServletRequest peticion) {
        return (Integer) atributo(peticion, CUENTA);
    }

    public static String documento(HttpServletRequest peticion) {
        return (String) atributo(peticion, DOCUMENTO);
    }

    public static String rol(HttpServletRequest peticion) {
        return (String) atributo(peticion, ROL);
    }

    private static Object atributo(HttpServletRequest peticion, String nombre) {
        HttpSession sesion = peticion.getSession(false);
        return sesion == null ? null : sesion.getAttribute(nombre);
    }
}
