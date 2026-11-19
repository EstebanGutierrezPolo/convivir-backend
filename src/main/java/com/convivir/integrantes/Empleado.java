package com.convivir.integrantes;

import java.time.LocalDate;

import com.convivir.comun.ReglaDeNegocioException;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Empleado extends Integrante {

    @Enumerated(EnumType.STRING)
    private CargoEmpleado cargoEmpleado;

    private LocalDate fechaIngreso;
    private LocalDate fechaFin;
    private String empresaSeguridad;
    private String turno;

    public Empleado(String documento, String nombres, String apellidos, CargoEmpleado cargoEmpleado) {
        super(documento, nombres, apellidos);
        this.cargoEmpleado = cargoEmpleado;
        this.fechaIngreso = LocalDate.now();
    }

    public void asignarDatosLaborales(LocalDate fechaIngreso, String empresaSeguridad, String turno) {
        this.fechaIngreso = fechaIngreso;
        this.empresaSeguridad = empresaSeguridad;
        this.turno = turno;
    }

    /** RF-014. Solo el administrador registra integrantes, en su misma copropiedad. */
    public void registrarIntegranteEnLaCopropiedad(Integrante integrante) {
        if (!validarQueElCargoSeaAdministrador()) {
            throw new ReglaDeNegocioException("Solo el administrador puede registrar integrantes.");
        }
        integrante.incorporarALaCopropiedad(getPropiedadHorizontal());
    }

    /** RF-017. Se desactiva y conserva su historial. */
    public void desactivarIntegrantePorMotivo(Integrante integrante, String motivo) {
        if (!validarQueElCargoSeaAdministrador()) {
            throw new ReglaDeNegocioException("Solo el administrador puede desactivar integrantes.");
        }
        if (integrante == this) {
            throw new ReglaDeNegocioException("No puedes desactivar tu propia cuenta de administrador.");
        }
        integrante.registrarDesactivacion(motivo);
    }

    @Override
    public String obtenerRolDelIntegrante() {
        return cargoEmpleado.name();
    }

    protected boolean validarQueElCargoSeaAdministrador() {
        return cargoEmpleado == CargoEmpleado.ADMINISTRADOR;
    }
}
