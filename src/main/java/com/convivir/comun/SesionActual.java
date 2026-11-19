package com.convivir.comun;

/**
 * Documento de quien hace la petición en curso, para que los eventos del
 * dominio digan quién actuó (EventoSistema.documentoActor).
 * No es una clase del dominio: es infraestructura, como los repositorios.
 * Mientras no exista el inicio de sesión (CU-01), el actor es "SISTEMA".
 */
public final class SesionActual {

    public static final String SISTEMA = "SISTEMA";

    private static final ThreadLocal<String> DOCUMENTO = ThreadLocal.withInitial(() -> SISTEMA);

    private SesionActual() {
    }

    public static String documentoDelActor() {
        return DOCUMENTO.get();
    }

    public static void asignarActor(String documento) {
        DOCUMENTO.set(documento == null ? SISTEMA : documento);
    }

    public static void limpiar() {
        DOCUMENTO.remove();
    }
}
