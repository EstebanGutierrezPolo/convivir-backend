package com.convivir.accesos;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.comun.SesionActual;
import com.convivir.eventos.EventoSistema;
import com.convivir.eventos.PublicadorEventos;
import com.convivir.inmuebles.Inmueble;
import com.convivir.integrantes.EstadoVigencia;
import com.convivir.integrantes.Integrante;
import com.convivir.reservas.DiaSemana;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Decisión 10, cerrada así: diasPermitidos guarda los nombres de DiaSemana
 * separados por coma ("LUNES,MIERCOLES") y horarios una franja "HH:mm-HH:mm".
 * Una visita ocasional no tiene días: vale entre fechaInicio y fechaFin.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AutorizacionAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private TipoAutorizacion tipoAutorizacion;

    private String nombreVisitante;
    private String documentoVisitante;
    private String motivoAcceso;
    private boolean usaParqueaderoVisitantes;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String diasPermitidos;
    private String horarios;

    @Enumerated(EnumType.STRING)
    private EstadoAutorizacion estado;

    // Inmueble "1" *-- "*" AutorizacionAcceso : recibe
    @ManyToOne(optional = false)
    private Inmueble inmueble;

    // Integrante "1" --> "*" AutorizacionAcceso : otorga
    @ManyToOne(optional = false)
    private Integrante autorizante;

    // AutorizacionAcceso "0..1" --> "*" MovimientoAcceso : habilita
    @OneToMany(mappedBy = "autorizacion")
    private List<MovimientoAcceso> movimientos = new ArrayList<>();

    public AutorizacionAcceso(String nombreVisitante, String documentoVisitante, TipoAutorizacion tipoAutorizacion) {
        this.nombreVisitante = nombreVisitante == null ? null : nombreVisitante.trim();
        this.documentoVisitante = Integrante.normalizarDocumento(documentoVisitante);
        this.tipoAutorizacion = tipoAutorizacion;
    }

    /** Periodo, días y franja en que vale la autorización, y si pide parqueadero. */
    public void definirVigencia(LocalDateTime fechaInicio, LocalDateTime fechaFin, String diasPermitidos,
            String horarios, String motivoAcceso, boolean usaParqueaderoVisitantes) {
        if (fechaInicio == null || fechaFin == null || !fechaFin.isAfter(fechaInicio)) {
            throw new ReglaDeNegocioException("La autorización termina después de empezar. Revisa las fechas y horas.");
        }
        if (tipoAutorizacion == TipoAutorizacion.RECURRENTE) {
            if (diasPermitidos == null || diasPermitidos.isBlank()) {
                throw new ReglaDeNegocioException("Elige al menos un día de la semana.");
            }
            leerDias(diasPermitidos);
            leerFranja(horarios);
        }
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.diasPermitidos = tipoAutorizacion == TipoAutorizacion.RECURRENTE ? diasPermitidos.toUpperCase() : null;
        this.horarios = tipoAutorizacion == TipoAutorizacion.RECURRENTE ? horarios.trim() : null;
        this.motivoAcceso = motivoAcceso;
        this.usaParqueaderoVisitantes = usaParqueaderoVisitantes;
    }

    /** RF-027, RF-036, RN-08. */
    public void registrarAutorizacionDeAcceso(Inmueble inmueble, Integrante autorizante) {
        if (nombreVisitante == null || nombreVisitante.isBlank() || documentoVisitante == null || documentoVisitante.isBlank()) {
            throw new ReglaDeNegocioException("Escribe el nombre y el documento del visitante.");
        }
        if (fechaInicio == null) {
            throw new ReglaDeNegocioException("Define cuándo vale la autorización.");
        }
        this.inmueble = inmueble;
        this.autorizante = autorizante;
        if (!validarQueElAutorizanteTengaPermiso()) {
            throw new ReglaDeNegocioException(autorizante.obtenerNombreCompleto()
                    + " no puede autorizar visitas en " + inmueble.getIdentificacion()
                    + ": necesita una vinculación vigente con permiso para autorizar accesos (RN-08).");
        }
        this.estado = EstadoAutorizacion.VIGENTE;
        inmueble.getAutorizaciones().add(this);
        publicar("AUTORIZACION_REGISTRADA");
    }

    /** RF-043. Vigente, dentro de sus fechas y, si es recurrente, en su día y franja. */
    public boolean validarVigenciaDeLaAutorizacion() {
        if (estado != EstadoAutorizacion.VIGENTE) {
            return false;
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isAfter(fechaFin)) {
            this.estado = EstadoAutorizacion.VENCIDA;
            return false;
        }
        if (ahora.isBefore(fechaInicio)) {
            return false;
        }
        return tipoAutorizacion == TipoAutorizacion.OCASIONAL || validarDiaYHorarioPermitido();
    }

    /** RF-030. Solo se cancela una visita que todavía no ha entrado. */
    public void cancelarAutorizacionOcasional() {
        if (tipoAutorizacion != TipoAutorizacion.OCASIONAL) {
            throw new ReglaDeNegocioException("Una autorización recurrente se revoca, no se cancela.");
        }
        if (estado != EstadoAutorizacion.VIGENTE) {
            throw new ReglaDeNegocioException("Solo se cancela una visita vigente que no ha entrado.");
        }
        this.estado = EstadoAutorizacion.CANCELADA;
        publicar("AUTORIZACION_CANCELADA");
    }

    /** RF-039. */
    public void revocarAutorizacionRecurrente() {
        if (tipoAutorizacion != TipoAutorizacion.RECURRENTE) {
            throw new ReglaDeNegocioException("Una visita ocasional se cancela, no se revoca.");
        }
        if (estado != EstadoAutorizacion.VIGENTE) {
            throw new ReglaDeNegocioException("La autorización ya no está vigente.");
        }
        this.estado = EstadoAutorizacion.REVOCADA;
        publicar("AUTORIZACION_REVOCADA");
    }

    protected boolean validarDiaYHorarioPermitido() {
        LocalDateTime ahora = LocalDateTime.now();
        DiaSemana hoy = DiaSemana.values()[ahora.getDayOfWeek().ordinal()];
        if (!leerDias(diasPermitidos).contains(hoy)) {
            return false;
        }
        LocalTime[] franja = leerFranja(horarios);
        LocalTime hora = ahora.toLocalTime();
        return !hora.isBefore(franja[0]) && !hora.isAfter(franja[1]);
    }

    protected boolean validarQueElAutorizanteTengaPermiso() {
        return autorizante.getVinculaciones().stream()
                .anyMatch(v -> v.getEstado() == EstadoVigencia.VIGENTE && v.isAutorizaAcceso()
                        && v.getInmueble().getId().equals(inmueble.getId()));
    }

    /** Una visita ocasional se usa una sola vez: al entrar queda UTILIZADA. */
    void registrarUso() {
        if (tipoAutorizacion == TipoAutorizacion.OCASIONAL) {
            this.estado = EstadoAutorizacion.UTILIZADA;
        }
    }

    private static List<DiaSemana> leerDias(String dias) {
        try {
            return Arrays.stream(dias.toUpperCase().split(",")).map(String::trim).map(DiaSemana::valueOf).toList();
        } catch (IllegalArgumentException e) {
            throw new ReglaDeNegocioException("Los días van separados por coma, por ejemplo LUNES,MIERCOLES.");
        }
    }

    private static LocalTime[] leerFranja(String franja) {
        try {
            String[] partes = franja.split("-");
            LocalTime inicio = LocalTime.parse(partes[0].trim());
            LocalTime fin = LocalTime.parse(partes[1].trim());
            if (!fin.isAfter(inicio)) {
                throw new ReglaDeNegocioException("La hora final de la franja va después de la inicial.");
            }
            return new LocalTime[] { inicio, fin };
        } catch (RuntimeException e) {
            if (e instanceof ReglaDeNegocioException regla) {
                throw regla;
            }
            throw new ReglaDeNegocioException("Escribe la franja como 07:00-17:00.");
        }
    }

    private void publicar(String tipoEvento) {
        PublicadorEventos.obtenerInstancia().publicar(new EventoSistema(tipoEvento,
                "Autorización de " + documentoVisitante + " para " + inmueble.getIdentificacion(),
                SesionActual.documentoDelActor()));
    }

}
