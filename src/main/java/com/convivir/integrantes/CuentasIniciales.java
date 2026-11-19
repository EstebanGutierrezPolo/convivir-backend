package com.convivir.integrantes;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.inmuebles.Inmueble;
import com.convivir.inmuebles.InmuebleRepositorio;
import com.convivir.inmuebles.NaturalezaInmueble;
import com.convivir.inmuebles.PropiedadHorizontal;
import com.convivir.inmuebles.PropiedadHorizontalRepositorio;
import com.convivir.inmuebles.TipoInmueble;
import com.convivir.inmuebles.TipoInmuebleRepositorio;

import lombok.extern.slf4j.Slf4j;

/**
 * Personas y cuentas de ejemplo para entrar con cada rol, una por rol más una
 * cuenta bloqueada. Solo se crean las que no existen.
 * La contraseña inicial NO está en el código: sale de la variable de entorno
 * CONVIVIR_CONTRASENA_INICIAL (archivo .env). Si no está, no se crea ninguna.
 * Los documentos y nombres son ficticios.
 */
@Slf4j
@Component
@Order(2)
public class CuentasIniciales implements ApplicationRunner {

    private final String contrasenaInicial;
    private final PropiedadHorizontalRepositorio propiedadHorizontalRepositorio;
    private final IntegranteRepositorio integranteRepositorio;
    private final CuentaRepositorio cuentaRepositorio;
    private final InmuebleRepositorio inmuebleRepositorio;
    private final TipoInmuebleRepositorio tipoInmuebleRepositorio;
    private final VinculacionRepositorio vinculacionRepositorio;

    public CuentasIniciales(@Value("${convivir.contrasena-inicial:}") String contrasenaInicial,
            PropiedadHorizontalRepositorio propiedadHorizontalRepositorio, IntegranteRepositorio integranteRepositorio,
            CuentaRepositorio cuentaRepositorio, InmuebleRepositorio inmuebleRepositorio,
            TipoInmuebleRepositorio tipoInmuebleRepositorio, VinculacionRepositorio vinculacionRepositorio) {
        this.contrasenaInicial = contrasenaInicial;
        this.propiedadHorizontalRepositorio = propiedadHorizontalRepositorio;
        this.integranteRepositorio = integranteRepositorio;
        this.cuentaRepositorio = cuentaRepositorio;
        this.inmuebleRepositorio = inmuebleRepositorio;
        this.tipoInmuebleRepositorio = tipoInmuebleRepositorio;
        this.vinculacionRepositorio = vinculacionRepositorio;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (contrasenaInicial.isBlank()) {
            log.warn("No se crearon cuentas de ejemplo: falta CONVIVIR_CONTRASENA_INICIAL en el archivo .env.");
            return;
        }
        if (contrasenaInicial.length() < 8) {
            log.warn("No se crearon cuentas de ejemplo: CONVIVIR_CONTRASENA_INICIAL debe tener al menos 8 caracteres.");
            return;
        }
        PropiedadHorizontal copropiedad = propiedadHorizontalRepositorio.findFirstByOrderByIdAsc().orElse(null);
        if (copropiedad == null) {
            return;
        }

        // La primera administradora se incorpora sola; las demás personas las registra ella.
        Empleado administradora = (Empleado) integranteRepositorio.findByDocumento("52345678").orElseGet(() -> {
            Empleado nueva = new Empleado("52345678", "Paula", "Ortega", CargoEmpleado.ADMINISTRADOR);
            nueva.incorporarALaCopropiedad(copropiedad);
            return integranteRepositorio.save(nueva);
        });
        cuenta(administradora, "paula.ortega");

        Empleado vigilante = new Empleado("1043765210", "Jhon", "Rojas", CargoEmpleado.VIGILANTE);
        vigilante.asignarDatosLaborales(LocalDate.of(2025, 6, 1), "Seguridad Atlántico", "noche");
        cuenta(registrar(administradora, vigilante), "jhon.rojas");

        cuenta(registrar(administradora, new MiembroConsejo("72198456", "Hernán", "Ruiz", "tesorero")), "hernan.ruiz");

        Inmueble apartamento = apartamento("B-0302", "Torre B");
        Integrante ernesto = registrar(administradora, new Propietario("80456123", "Ernesto", "Vargas"));
        vincular(ernesto, apartamento, false, 100);
        Integrante lucia = registrar(administradora, new Arrendatario("1045678912", "Lucía", "Mendoza", "ARR-2026-0187"));
        vincular(lucia, apartamento, true, 0);
        cuenta(lucia, "lucia.mendoza");

        // Una cuenta bloqueada por intentos fallidos, para ver ese caso en el inicio de sesión.
        Integrante marta = registrar(administradora, new Propietario("32876540", "Marta", "Oviedo"));
        Cuenta deMarta = cuenta(marta, "marta.oviedo");
        if (deMarta != null) {
            for (int i = 0; i < Cuenta.MAXIMO_INTENTOS; i++) {
                deMarta.iniciarSesionConLoginYContrasena("intento-equivocado");
            }
        }
    }

    private Integrante registrar(Empleado administradora, Integrante integrante) {
        return integranteRepositorio.findByDocumento(integrante.getDocumento()).orElseGet(() -> {
            administradora.registrarIntegranteEnLaCopropiedad(integrante);
            return integranteRepositorio.save(integrante);
        });
    }

    /** Crea la cuenta si el login no existe. Devuelve la cuenta nueva, o null si ya existía. */
    private Cuenta cuenta(Integrante integrante, String login) {
        if (cuentaRepositorio.existsByLogin(login) || integrante.getCuenta() != null) {
            return null;
        }
        Cuenta cuenta = new Cuenta(login, contrasenaInicial);
        cuenta.asignarAlIntegrante(integrante);
        cuentaRepositorio.save(cuenta);
        log.info("Cuenta de ejemplo creada: {}", login);
        return cuenta;
    }

    private void vincular(Integrante integrante, Inmueble inmueble, boolean esResidente, double porcentaje) {
        boolean yaVinculado = integrante.getVinculaciones().stream()
                .anyMatch(v -> v.getEstado() == EstadoVigencia.VIGENTE && v.getInmueble().getId().equals(inmueble.getId()));
        if (yaVinculado) {
            return;
        }
        try {
            Vinculacion vinculacion = new Vinculacion(esResidente, porcentaje, LocalDate.of(2026, 3, 1));
            vinculacion.vincularIntegranteAlInmueble(integrante, inmueble);
            vinculacionRepositorio.save(vinculacion);
        } catch (ReglaDeNegocioException e) {
            log.warn("No se vinculó {} a {}: {}", integrante.obtenerNombreCompleto(), inmueble.getIdentificacion(), e.getMessage());
        }
    }

    /** El apartamento de ejemplo y su torre, creados si no existen. */
    private Inmueble apartamento(String identificacion, String torre) {
        return inmuebleRepositorio.findByIdentificacionIgnoreCase(identificacion).orElseGet(() -> {
            Inmueble contenedor = inmuebleRepositorio.findByIdentificacionIgnoreCase(torre)
                    .orElseGet(() -> nuevo(torre, 0, 2400, NaturalezaInmueble.USO_GENERAL, "Torre", null));
            return nuevo(identificacion, 0.0052, 68.4, NaturalezaInmueble.BIEN_PRIVADO, "Apartamento", contenedor);
        });
    }

    private Inmueble nuevo(String identificacion, double coeficiente, double area, NaturalezaInmueble naturaleza,
            String tipo, Inmueble contenedor) {
        TipoInmueble tipoInmueble = tipoInmuebleRepositorio.findAllByOrderByNombreAsc().stream()
                .filter(t -> t.getNombre().equals(tipo)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Falta el tipo de inmueble " + tipo));
        Inmueble inmueble = new Inmueble(identificacion, coeficiente, area, naturaleza, tipoInmueble, contenedor);
        inmueble.asignarPropiedadHorizontal(propiedadHorizontalRepositorio.findFirstByOrderByIdAsc().orElseThrow());
        inmueble.registrarInmueble();
        return inmuebleRepositorio.save(inmueble);
    }
}
