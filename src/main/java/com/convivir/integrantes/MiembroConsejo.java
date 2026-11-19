package com.convivir.integrantes;

import java.time.LocalDate;

import com.convivir.comun.ReglaDeNegocioException;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiembroConsejo extends Integrante {

    private String cargoEnConsejo;
    private LocalDate periodoInicio;
    private LocalDate periodoFin;

    public MiembroConsejo(String documento, String nombres, String apellidos, String cargoEnConsejo) {
        super(documento, nombres, apellidos);
        this.cargoEnConsejo = cargoEnConsejo;
    }

    public void asignarPeriodoEnConsejo(LocalDate periodoInicio, LocalDate periodoFin) {
        if (periodoInicio != null && periodoFin != null && periodoFin.isBefore(periodoInicio)) {
            throw new ReglaDeNegocioException("El periodo en el consejo termina antes de empezar. Revisa las fechas.");
        }
        this.periodoInicio = periodoInicio;
        this.periodoFin = periodoFin;
    }

    @Override
    public String obtenerRolDelIntegrante() {
        return "MIEMBRO_CONSEJO";
    }
}
