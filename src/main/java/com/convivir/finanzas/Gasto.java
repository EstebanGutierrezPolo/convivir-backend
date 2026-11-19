package com.convivir.finanzas;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.inmuebles.PropiedadHorizontal;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Decisión 5: no hay requisito para registrar gastos; se cargan como datos iniciales. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String concepto;
    private double valor;
    private LocalDate fecha;

    // PropiedadHorizontal "1" --> "*" Gasto : asume
    @ManyToOne(optional = false)
    private PropiedadHorizontal propiedadHorizontal;

    public Gasto(String concepto, double valor, LocalDate fecha, PropiedadHorizontal propiedadHorizontal) {
        this.concepto = concepto;
        this.valor = valor;
        this.fecha = fecha;
        this.propiedadHorizontal = propiedadHorizontal;
    }

    /** RF-089. Gastos de un mes, por ejemplo "2026-11". */
    public static List<Gasto> consultarGastosDelPeriodo(List<Gasto> gastos, String periodo) {
        YearMonth mes;
        try {
            mes = YearMonth.parse(periodo);
        } catch (RuntimeException e) {
            throw new ReglaDeNegocioException("Escribe el periodo como año y mes, por ejemplo 2026-11.");
        }
        return gastos.stream().filter(g -> YearMonth.from(g.fecha).equals(mes)).toList();
    }
}
