package com.convivir.integrantes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.convivir.accesos.AutorizacionAcceso;
import com.convivir.accesos.MovimientoAcceso;
import com.convivir.cesiones.Vehiculo;
import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.comun.SesionActual;
import com.convivir.eventos.Auditoria;
import com.convivir.eventos.EventoSistema;
import com.convivir.eventos.Notificacion;
import com.convivir.eventos.PublicadorEventos;
import com.convivir.inmuebles.EstadoRegistro;
import com.convivir.inmuebles.PropiedadHorizontal;
import com.convivir.reportes.Reporte;
import com.convivir.reservas.Reserva;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Decisión 1, cerrada: en el diagrama era una interfaz con atributos, que en
 * Java no guarda datos. Es una clase abstracta y cada tipo de integrante la
 * hereda. JOINED: una tabla con los datos comunes y una por cada tipo.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Integrante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String tipoDocumento;

    @Column(nullable = false, unique = true)
    private String documento;

    private String nombres;
    private String apellidos;
    private String razonSocial;

    @Enumerated(EnumType.STRING)
    private Genero genero;

    private LocalDate fechaNacimiento;
    private String paisResidencia;
    private String ciudadResidencia;
    private String direccionResidencia;
    private String telefono;
    private String correo;

    @Enumerated(EnumType.STRING)
    private EstadoRegistro estado;

    private String motivoDesactivacion;

    // PropiedadHorizontal "1" o-- "*" Integrante : agrupa
    @ManyToOne
    private PropiedadHorizontal propiedadHorizontal;

    // Integrante "1" --> "*" Vinculacion : tiene
    @OneToMany(mappedBy = "integrante")
    private List<Vinculacion> vinculaciones = new ArrayList<>();

    // Integrante "1" *-- "0..1" Cuenta : accede con (composición uno a uno)
    @OneToOne(mappedBy = "integrante", cascade = CascadeType.ALL, orphanRemoval = true)
    private Cuenta cuenta;

    // Integrante "0..1" --> "*" Vehiculo : responde por
    @OneToMany(mappedBy = "responsable")
    private List<Vehiculo> vehiculos = new ArrayList<>();

    // Integrante "1" --> "*" AutorizacionAcceso : otorga
    @OneToMany(mappedBy = "autorizante")
    private List<AutorizacionAcceso> autorizacionesOtorgadas = new ArrayList<>();

    // Integrante "1" --> "*" MovimientoAcceso : registra
    @OneToMany(mappedBy = "registradoPor")
    private List<MovimientoAcceso> movimientosRegistrados = new ArrayList<>();

    // Integrante "1" --> "*" Reserva : realiza
    @OneToMany(mappedBy = "integrante")
    private List<Reserva> reservas = new ArrayList<>();

    // Integrante "1" --> "*" Reporte : crea
    @OneToMany(mappedBy = "creadoPor")
    private List<Reporte> reportesCreados = new ArrayList<>();

    // Integrante "0..1" --> "*" Reporte : atiende
    @OneToMany(mappedBy = "responsable")
    private List<Reporte> reportesAtendidos = new ArrayList<>();

    // Integrante "1" --> "*" Notificacion : recibe
    @OneToMany(mappedBy = "destinatario")
    private List<Notificacion> notificaciones = new ArrayList<>();

    // Integrante "1" --> "*" Auditoria : realiza
    @OneToMany(mappedBy = "integrante")
    private List<Auditoria> auditorias = new ArrayList<>();

    protected Integrante(String documento, String nombres, String apellidos) {
        this.documento = normalizarDocumento(documento);
        this.nombres = nombres == null ? null : nombres.trim();
        this.apellidos = apellidos == null ? null : apellidos.trim();
    }

    /** Datos personales que no van en el constructor (RF-014). */
    public void completarDatosPersonales(String tipoDocumento, String razonSocial, Genero genero,
            LocalDate fechaNacimiento, String paisResidencia, String ciudadResidencia) {
        if (fechaNacimiento != null && fechaNacimiento.isAfter(LocalDate.now())) {
            throw new ReglaDeNegocioException("La fecha de nacimiento no puede ser futura.");
        }
        this.tipoDocumento = tipoDocumento;
        this.razonSocial = razonSocial;
        this.genero = genero;
        this.fechaNacimiento = fechaNacimiento;
        this.paisResidencia = paisResidencia;
        this.ciudadResidencia = ciudadResidencia;
    }

    /** RF-016. */
    public void modificarDatosDeContacto(String telefono, String correo, String direccionResidencia) {
        if (correo != null && !correo.isBlank() && !correo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new ReglaDeNegocioException("El correo " + correo + " no es válido. Revisa que tenga @ y un dominio.");
        }
        this.telefono = telefono;
        this.correo = correo == null ? null : correo.trim().toLowerCase();
        this.direccionResidencia = direccionResidencia;
        publicar("DATOS_DE_CONTACTO_MODIFICADOS");
    }

    public String obtenerNombreCompleto() {
        if (razonSocial != null && !razonSocial.isBlank()) {
            return razonSocial;
        }
        return (nombres + " " + (apellidos == null ? "" : apellidos)).trim();
    }

    /**
     * Quita puntos, espacios y guiones y deja las letras en mayúscula, para que
     * "1.045.887.332" y "1045887332" sean el mismo documento. Conserva las
     * letras de pasaportes y cédulas de extranjería (AB123456).
     */
    public static String normalizarDocumento(String documento) {
        return documento == null ? null : documento.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    /** RF-102. El rol sale del tipo de integrante. */
    public abstract String obtenerRolDelIntegrante();

    // Solo los usa Empleado, que es quien registra y desactiva integrantes.
    void incorporarALaCopropiedad(PropiedadHorizontal propiedadHorizontal) {
        if (documento == null || documento.isBlank() || nombres == null || nombres.isBlank()) {
            throw new ReglaDeNegocioException("El número de documento y los nombres son obligatorios.");
        }
        this.propiedadHorizontal = propiedadHorizontal;
        this.estado = EstadoRegistro.ACTIVO;
        publicar("INTEGRANTE_REGISTRADO");
    }

    void registrarDesactivacion(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException("Escribe el motivo de la desactivación.");
        }
        if (estado == EstadoRegistro.INACTIVO) {
            throw new ReglaDeNegocioException(obtenerNombreCompleto() + " ya está desactivado.");
        }
        this.estado = EstadoRegistro.INACTIVO;
        this.motivoDesactivacion = motivo.trim();
        publicar("INTEGRANTE_DESACTIVADO");
    }

    void asignarCuenta(Cuenta cuenta) {
        this.cuenta = cuenta;
    }

    protected void publicar(String tipoEvento) {
        PublicadorEventos.obtenerInstancia().publicar(new EventoSistema(tipoEvento,
                "Integrante " + documento, SesionActual.documentoDelActor()));
    }
}
