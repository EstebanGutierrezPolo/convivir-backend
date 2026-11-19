package com.convivir.inmuebles;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.convivir.accesos.AutorizacionAcceso;
import com.convivir.accesos.MovimientoAcceso;
import com.convivir.cesiones.CesionUso;
import com.convivir.cesiones.Vehiculo;
import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.comun.SesionActual;
import com.convivir.eventos.EventoSistema;
import com.convivir.eventos.PublicadorEventos;
import com.convivir.finanzas.EstadoObligacion;
import com.convivir.finanzas.Obligacion;
import com.convivir.finanzas.Pago;
import com.convivir.integrantes.EstadoVigencia;
import com.convivir.integrantes.Vinculacion;
import com.convivir.reportes.Reporte;
import com.convivir.reservas.AsignacionInmueble;
import com.convivir.reservas.ProgramacionAsignacion;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Todo se ancla al inmueble: vinculaciones, obligaciones, pagos, reportes y
 * movimientos se consultan desde aquí aunque cambien sus ocupantes (RF-013).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inmueble {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String identificacion;

    private double coeficiente;
    private double areaM2;

    @Enumerated(EnumType.STRING)
    private NaturalezaInmueble naturalezaInmueble;

    @Enumerated(EnumType.STRING)
    private EstadoRegistro estado;

    private String motivoDesactivacion;

    // TipoInmueble "1" <-- "*" Inmueble : es de tipo
    @ManyToOne(optional = false)
    private TipoInmueble tipoInmueble;

    // Inmueble "0..1" o-- "*" Inmueble : contiene (reflexiva)
    @ManyToOne
    private Inmueble inmuebleContenedor;

    @OneToMany(mappedBy = "inmuebleContenedor")
    private List<Inmueble> inmueblesContenidos = new ArrayList<>();

    // PropiedadHorizontal "1" o-- "*" Inmueble : administra
    @ManyToOne
    private PropiedadHorizontal propiedadHorizontal;

    // PropiedadHorizontal "0..1" --> "*" Inmueble : es propietaria de
    @ManyToOne
    private PropiedadHorizontal propiedadPropietaria;

    // Vinculacion "*" --> "1" Inmueble : registra (recorrida al revés para el historial)
    @OneToMany(mappedBy = "inmueble")
    private List<Vinculacion> vinculaciones = new ArrayList<>();

    // Inmueble "1" --> "*" AsignacionInmueble : se asigna en
    @OneToMany(mappedBy = "inmueble")
    private List<AsignacionInmueble> asignaciones = new ArrayList<>();

    // Inmueble "1" --> "*" CesionUso : cede
    @OneToMany(mappedBy = "inmuebleQueCede")
    private List<CesionUso> cesionesQueCede = new ArrayList<>();

    // Inmueble "1" --> "*" CesionUso : usa
    @OneToMany(mappedBy = "inmuebleQueUsa")
    private List<CesionUso> cesionesQueUsa = new ArrayList<>();

    // Inmueble "1" o-- "*" Vehiculo : posee
    @OneToMany(mappedBy = "inmueble")
    private List<Vehiculo> vehiculos = new ArrayList<>();

    // Inmueble "1" *-- "*" AutorizacionAcceso : recibe (composición)
    @OneToMany(mappedBy = "inmueble", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AutorizacionAcceso> autorizaciones = new ArrayList<>();

    // Inmueble "0..1" o-- "*" MovimientoAcceso : recibe
    @OneToMany(mappedBy = "inmuebleDestino")
    private List<MovimientoAcceso> movimientos = new ArrayList<>();

    // Inmueble "1" *-- "*" Obligacion : adeuda (composición)
    @OneToMany(mappedBy = "inmueble", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Obligacion> obligaciones = new ArrayList<>();

    // Inmueble "1" *-- "*" Pago : realiza (composición)
    @OneToMany(mappedBy = "inmueble", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pago> pagos = new ArrayList<>();

    // Inmueble "0..1" *-- "*" Reporte : origina (composición)
    @OneToMany(mappedBy = "inmueble", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reporte> reportes = new ArrayList<>();

    public Inmueble(String identificacion, double coeficiente, double areaM2,
            NaturalezaInmueble naturalezaInmueble, TipoInmueble tipoInmueble) {
        this(identificacion, coeficiente, areaM2, naturalezaInmueble, tipoInmueble, null);
    }

    public Inmueble(String identificacion, double coeficiente, double areaM2,
            NaturalezaInmueble naturalezaInmueble, TipoInmueble tipoInmueble, Inmueble inmuebleContenedor) {
        this.identificacion = identificacion == null ? null : identificacion.trim();
        this.coeficiente = coeficiente;
        this.areaM2 = areaM2;
        this.naturalezaInmueble = naturalezaInmueble;
        this.tipoInmueble = tipoInmueble;
        this.inmuebleContenedor = inmuebleContenedor;
    }

    /** RF-006. El inmueble queda activo en la copropiedad que lo administra. */
    public void registrarInmueble() {
        if (identificacion == null || identificacion.isBlank()) {
            throw new ReglaDeNegocioException("Escribe la identificación del inmueble, por ejemplo A-1203.");
        }
        if (tipoInmueble == null || tipoInmueble.getEstado() != EstadoRegistro.ACTIVO) {
            throw new ReglaDeNegocioException("Elige un tipo de inmueble activo.");
        }
        validarMedidas(coeficiente, areaM2);
        this.estado = EstadoRegistro.ACTIVO;
        publicar("INMUEBLE_REGISTRADO");
    }

    /** Asigna la copropiedad que lo administra y, si es bien común, la que es su dueña. */
    public void asignarPropiedadHorizontal(PropiedadHorizontal propiedadHorizontal) {
        this.propiedadHorizontal = propiedadHorizontal;
        this.propiedadPropietaria = tipoInmueble != null && tipoInmueble.isEsBienComun() ? propiedadHorizontal : null;
    }

    /** RF-010. */
    public void modificarDatosDelInmueble(double coeficiente, double areaM2, NaturalezaInmueble naturalezaInmueble) {
        validarMedidas(coeficiente, areaM2);
        this.coeficiente = coeficiente;
        this.areaM2 = areaM2;
        this.naturalezaInmueble = naturalezaInmueble;
        publicar("INMUEBLE_MODIFICADO");
    }

    /** RF-011, RN-04. No se borra: se desactiva y conserva su historial. */
    public void desactivarInmueblePorMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException("Escribe el motivo de la desactivación. Queda en el historial del inmueble.");
        }
        if (estado == EstadoRegistro.INACTIVO) {
            throw new ReglaDeNegocioException("El inmueble " + identificacion + " ya está desactivado.");
        }
        this.estado = EstadoRegistro.INACTIVO;
        this.motivoDesactivacion = motivo.trim();
        publicar("INMUEBLE_DESACTIVADO");
    }

    /** RF-012. */
    public void reactivarInmuebleDesactivado() {
        if (estado != EstadoRegistro.INACTIVO) {
            throw new ReglaDeNegocioException("El inmueble " + identificacion + " ya está activo.");
        }
        this.estado = EstadoRegistro.ACTIVO;
        this.motivoDesactivacion = null;
        publicar("INMUEBLE_REACTIVADO");
    }

    // ---------- Historial del inmueble (RF-013) ----------

    public List<Vinculacion> consultarHistorialDeVinculacionesDelInmueble() {
        return vinculaciones.stream()
                .sorted(Comparator.comparing(Vinculacion::getFechaInicio).reversed())
                .toList();
    }

    public List<Obligacion> consultarHistorialDeObligacionesDelInmueble() {
        return obligaciones.stream()
                .sorted(Comparator.comparing(Obligacion::getFechaVencimiento).reversed())
                .toList();
    }

    public List<Pago> consultarHistorialDePagosDelInmueble() {
        return pagos.stream()
                .sorted(Comparator.comparing(Pago::getFecha).reversed())
                .toList();
    }

    public List<Reporte> consultarHistorialDeReportesDelInmueble() {
        return reportes.stream()
                .sorted(Comparator.comparing(Reporte::getFechaCreacion).reversed())
                .toList();
    }

    public List<MovimientoAcceso> consultarHistorialDeMovimientosDelInmueble() {
        return movimientos.stream()
                .sorted(Comparator.comparing(MovimientoAcceso::getFechaHora).reversed())
                .toList();
    }

    // ---------- Finanzas ----------

    /** Suma lo que falta por pagar de las obligaciones que no están pagadas ni anuladas. */
    public double calcularSaldoPendienteDelInmueble() {
        return obligaciones.stream()
                .filter(o -> o.getEstado() != EstadoObligacion.ANULADA && o.getEstado() != EstadoObligacion.PAGADA)
                .mapToDouble(Obligacion::getSaldoPendiente)
                .sum();
    }

    /**
     * Cuota ordinaria según el coeficiente (Ley 675): el valor base del periodo
     * se reparte entre las unidades en proporción a su coeficiente.
     */
    public double calcularCuotaDelInmueble(double valorBaseCuota) {
        if (estado != EstadoRegistro.ACTIVO || !tipoInmueble.isGeneraCuota()) {
            return 0;
        }
        return Math.round(valorBaseCuota * coeficiente);
    }

    /** RF-120. Solo se expide si no hay nada vencido a la fecha de corte. */
    public String expedirPazYSalvoAFechaDeCorte(LocalDate fechaCorte) {
        double vencido = obligaciones.stream()
                .filter(o -> o.getEstado() != EstadoObligacion.ANULADA && o.getEstado() != EstadoObligacion.PAGADA)
                .filter(o -> !o.getFechaVencimiento().isAfter(fechaCorte))
                .mapToDouble(Obligacion::getSaldoPendiente)
                .sum();
        String fecha = fechaCorte.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.of("es", "CO")));
        if (vencido > 0) {
            throw new ReglaDeNegocioException("No se puede expedir el paz y salvo: el inmueble " + identificacion
                    + " debe " + NumberFormat.getCurrencyInstance(Locale.of("es", "CO")).format(vencido)
                    + " vencidos al " + fecha + ".");
        }
        publicar("PAZ_Y_SALVO_EXPEDIDO");
        String copropiedad = propiedadHorizontal == null ? "La copropiedad" : propiedadHorizontal.getNombre();
        return copropiedad + " certifica que el inmueble " + identificacion
                + " se encuentra a paz y salvo por todo concepto al " + fecha + ".";
    }

    // ---------- Uso y disponibilidad ----------

    /**
     * RF-063. Quién usa hoy este inmueble: el que lo recibió en una cesión
     * vigente o, si no está cedido, el mismo inmueble.
     */
    public Inmueble obtenerInmuebleConUsoVigente() {
        LocalDate hoy = LocalDate.now();
        return cesionesQueCede.stream()
                .filter(c -> c.getEstado() == EstadoVigencia.VIGENTE)
                .filter(c -> !c.getFechaInicio().isAfter(hoy) && (c.getFechaFin() == null || !c.getFechaFin().isBefore(hoy)))
                .map(CesionUso::getInmuebleQueUsa)
                .findFirst()
                .orElse(this);
    }

    /**
     * RF-115. Libre si, en cada día del periodo, ninguna asignación vigente
     * tiene una franja de ese día de la semana que se cruce con las horas pedidas.
     */
    public boolean obtenerDisponibilidadEnPeriodo(LocalDate fechaInicio, LocalDate fechaFin,
            LocalTime horaInicio, LocalTime horaFin) {
        for (AsignacionInmueble asignacion : AsignacionInmueble.consultarPeriodosOcupadosDelInmueble(this)) {
            for (LocalDate dia = fechaInicio; !dia.isAfter(fechaFin); dia = dia.plusDays(1)) {
                if (dia.isBefore(asignacion.getFechaInicio()) || dia.isAfter(asignacion.getFechaFin())) {
                    continue;
                }
                int diaDeLaSemana = dia.getDayOfWeek().ordinal();
                for (ProgramacionAsignacion franja : asignacion.getProgramaciones()) {
                    if (franja.getDiaSemana().ordinal() == diaDeLaSemana
                            && franja.getHoraInicio().isBefore(horaFin) && horaInicio.isBefore(franja.getHoraFin())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void validarMedidas(double coeficiente, double areaM2) {
        if (coeficiente < 0 || coeficiente > 1) {
            NumberFormat numero = NumberFormat.getNumberInstance(Locale.of("es", "CO"));
            numero.setMaximumFractionDigits(6);
            throw new ReglaDeNegocioException("El coeficiente va entre 0 y 1. Para " + numero.format(coeficiente)
                    + " por ciento escribe " + numero.format(coeficiente / 100) + ".");
        }
        if (areaM2 <= 0) {
            throw new ReglaDeNegocioException("El área debe ser mayor que cero, en metros cuadrados.");
        }
    }

    private void publicar(String tipoEvento) {
        PublicadorEventos.obtenerInstancia()
                .publicar(new EventoSistema(tipoEvento, "Inmueble " + identificacion, SesionActual.documentoDelActor()));
    }
}
