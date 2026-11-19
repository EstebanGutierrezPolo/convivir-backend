package com.convivir.finanzas;

/** Patrón Estrategia: cómo se calcula el interés de mora de un saldo. */
public interface PoliticaInteresMora {
    double calcularInteres(double saldo, int diasMora);
}
