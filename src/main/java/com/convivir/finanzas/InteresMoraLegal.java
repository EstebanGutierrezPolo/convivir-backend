package com.convivir.finanzas;

/**
 * Interés de mora simple a la tasa mensual máxima legal (Ley 675, art. 30).
 * La tasa cambia cada mes con la certificación de la Superfinanciera; aquí es
 * un valor de referencia que se debe actualizar.
 */
public class InteresMoraLegal implements PoliticaInteresMora {

    public static final double TASA_MENSUAL = 0.0204;

    @Override
    public double calcularInteres(double saldo, int diasMora) {
        if (saldo <= 0 || diasMora <= 0) {
            return 0;
        }
        return Math.round(saldo * TASA_MENSUAL * diasMora / 30.0);
    }
}
