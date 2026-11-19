package com.convivir.finanzas;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.comun.SesionActual;
import com.convivir.eventos.EventoSistema;
import com.convivir.eventos.PublicadorEventos;
import com.convivir.inmuebles.EstadoRegistro;
import com.convivir.inmuebles.Inmueble;
import com.convivir.inmuebles.PropiedadHorizontal;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Un mes de facturación, por ejemplo "2026-11". */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PeriodoFacturacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String periodo;

    private double valorBaseCuota;

    // PropiedadHorizontal "1" *-- "*" PeriodoFacturacion : factura en
    @ManyToOne(optional = false)
    private PropiedadHorizontal propiedadHorizontal;

    // PeriodoFacturacion "0..1" --> "*" Obligacion : genera
    @OneToMany(mappedBy = "periodoFacturacion")
    private List<Obligacion> obligaciones = new ArrayList<>();

    public PeriodoFacturacion(String periodo, double valorBaseCuota, PropiedadHorizontal propiedadHorizontal) {
        try {
            YearMonth.parse(periodo);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ReglaDeNegocioException("Escribe el periodo como año y mes, por ejemplo 2026-11.");
        }
        this.periodo = periodo;
        this.propiedadHorizontal = propiedadHorizontal;
        configurarValorBaseDeLaCuotaDelPeriodo(valorBaseCuota);
    }

    /** RF-065. Se puede ajustar mientras el periodo no se haya generado. */
    public void configurarValorBaseDeLaCuotaDelPeriodo(double valorBaseCuota) {
        if (valorBaseCuota <= 0) {
            throw new ReglaDeNegocioException("El valor base de la cuota debe ser mayor que cero.");
        }
        if (!obligaciones.isEmpty()) {
            throw new ReglaDeNegocioException("Las obligaciones de " + periodo
                    + " ya se generaron. Ajusta cada una o crea una obligación individual.");
        }
        this.valorBaseCuota = valorBaseCuota;
    }

    /** RF-066. Una cuota por inmueble activo que genera cuota, según su coeficiente. Vence el último día del mes. */
    public void generarObligacionesDelPeriodoParaInmueblesActivos() {
        if (!obligaciones.isEmpty()) {
            throw new ReglaDeNegocioException("Las obligaciones de " + periodo + " ya se generaron.");
        }
        LocalDate vencimiento = YearMonth.parse(periodo).atEndOfMonth();
        FabricaObligacion fabrica = new FabricaObligacion();
        for (Inmueble inmueble : propiedadHorizontal.getInmuebles()) {
            if (inmueble.getEstado() != EstadoRegistro.ACTIVO || !inmueble.getTipoInmueble().isGeneraCuota()) {
                continue;
            }
            double cuota = inmueble.calcularCuotaDelInmueble(valorBaseCuota);
            if (cuota <= 0) {
                continue;
            }
            Obligacion obligacion = fabrica.crearCuotaOrdinaria(cuota, vencimiento);
            obligacion.describir("Cuota de administración " + periodo);
            obligacion.asignarPeriodo(this);
            obligacion.cargarAlInmueble(inmueble);
            obligaciones.add(obligacion);
        }
        if (obligaciones.isEmpty()) {
            throw new ReglaDeNegocioException("No hay inmuebles activos que paguen cuota.");
        }
        PublicadorEventos.obtenerInstancia().publicar(new EventoSistema("OBLIGACIONES_DEL_PERIODO_GENERADAS",
                "Periodo " + periodo + ": " + obligaciones.size() + " obligaciones", SesionActual.documentoDelActor()));
    }
}
