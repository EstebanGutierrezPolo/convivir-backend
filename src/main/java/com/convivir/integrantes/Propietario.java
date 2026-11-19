package com.convivir.integrantes;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Propietario extends Integrante {

    private String direccionCorrespondencia;

    public Propietario(String documento, String nombres, String apellidos) {
        super(documento, nombres, apellidos);
    }

    public void asignarDireccionCorrespondencia(String direccionCorrespondencia) {
        this.direccionCorrespondencia = direccionCorrespondencia;
    }

    @Override
    public String obtenerRolDelIntegrante() {
        return "PROPIETARIO";
    }
}
