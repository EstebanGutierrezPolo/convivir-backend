package com.convivir.integrantes;

import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.comun.SesionActual;
import com.convivir.eventos.EventoSistema;
import com.convivir.eventos.PublicadorEventos;
import com.convivir.inmuebles.Inmueble;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** La contraseña nunca se guarda en claro: se guarda su huella BCrypt. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cuenta {

    /** RNF-01. Intentos fallidos seguidos antes de bloquear la cuenta. */
    public static final int MAXIMO_INTENTOS = 3;

    private static final BCryptPasswordEncoder CIFRADOR = new BCryptPasswordEncoder();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String login;

    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private String contrasena;

    @Getter(AccessLevel.NONE)
    private int intentosFallidos;

    @Enumerated(EnumType.STRING)
    private EstadoCuenta estado;

    private String motivoDesactivacion;

    // Integrante "1" *-- "0..1" Cuenta : accede con
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    private Integrante integrante;

    public Cuenta(String login, String contrasena) {
        this.login = login == null ? null : login.trim().toLowerCase();
        this.contrasena = cifrar(contrasena);
        this.estado = EstadoCuenta.ACTIVA;
    }

    /** CU-18. La cuenta queda ligada al integrante y él a ella. */
    public void asignarAlIntegrante(Integrante integrante) {
        if (integrante.getCuenta() != null) {
            throw new ReglaDeNegocioException(integrante.obtenerNombreCompleto() + " ya tiene una cuenta.");
        }
        this.integrante = integrante;
        integrante.asignarCuenta(this);
        publicar("CUENTA_CREADA");
    }

    /** RF-001, RNF-01. */
    public boolean iniciarSesionConLoginYContrasena(String contrasenaIngresada) {
        if (estado != EstadoCuenta.ACTIVA) {
            return false;
        }
        if (!validarContrasenaIngresada(contrasenaIngresada)) {
            intentosFallidos++;
            if (intentosFallidos >= MAXIMO_INTENTOS) {
                bloquearCuentaPorIntentosFallidos();
            }
            return false;
        }
        intentosFallidos = 0;
        publicar("INICIO_SESION");
        return true;
    }

    /** RF-004. */
    public void cerrarSesionActiva() {
        publicar("CIERRE_SESION");
    }

    /** RF-002. También desbloquea una cuenta bloqueada por intentos. */
    public void restablecerContrasena(String nuevaContrasena) {
        if (estado == EstadoCuenta.INACTIVA) {
            throw new ReglaDeNegocioException("La cuenta está desactivada. Pide a la administración que la reactive.");
        }
        this.contrasena = cifrar(nuevaContrasena);
        this.intentosFallidos = 0;
        this.estado = EstadoCuenta.ACTIVA;
        publicar("CONTRASENA_RESTABLECIDA");
    }

    public void desactivarCuentaPorMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException("Escribe el motivo de la desactivación.");
        }
        this.estado = EstadoCuenta.INACTIVA;
        this.motivoDesactivacion = motivo.trim();
        publicar("CUENTA_DESACTIVADA");
    }

    public String obtenerRolDelIntegrante() {
        return integrante.obtenerRolDelIntegrante();
    }

    /** El administrador y el vigilante ven toda la copropiedad; los demás, sus inmuebles vinculados. */
    public List<Inmueble> obtenerInmueblesAccesibles() {
        if (integrante instanceof Empleado) {
            return integrante.getPropiedadHorizontal().getInmuebles();
        }
        return integrante.getVinculaciones().stream()
                .filter(v -> v.getEstado() == EstadoVigencia.VIGENTE)
                .map(Vinculacion::getInmueble)
                .toList();
    }

    /** Decisión 7: PROPIETARIO_RESIDENTE, ARRENDATARIO_NO_RESIDENTE, etc. */
    public String obtenerRolDelIntegranteEnInmueble(Inmueble inmueble) {
        return integrante.getVinculaciones().stream()
                .filter(v -> v.getEstado() == EstadoVigencia.VIGENTE && v.getInmueble().getId().equals(inmueble.getId()))
                .findFirst()
                .map(v -> integrante.obtenerRolDelIntegrante() + (v.isEsResidente() ? "_RESIDENTE" : "_NO_RESIDENTE"))
                .orElseThrow(() -> new ReglaDeNegocioException(integrante.obtenerNombreCompleto()
                        + " no tiene una vinculación vigente con " + inmueble.getIdentificacion() + "."));
    }

    private boolean validarContrasenaIngresada(String contrasenaIngresada) {
        return contrasenaIngresada != null && CIFRADOR.matches(contrasenaIngresada, contrasena);
    }

    private void bloquearCuentaPorIntentosFallidos() {
        this.estado = EstadoCuenta.BLOQUEADA;
        publicar("CUENTA_BLOQUEADA");
    }

    private static String cifrar(String contrasena) {
        if (contrasena == null || contrasena.length() < 8) {
            throw new ReglaDeNegocioException("La contraseña debe tener al menos 8 caracteres.");
        }
        return CIFRADOR.encode(contrasena);
    }

    private void publicar(String tipoEvento) {
        PublicadorEventos.obtenerInstancia().publicar(new EventoSistema(tipoEvento, "Cuenta " + login,
                SesionActual.documentoDelActor()));
    }
}
