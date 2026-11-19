package com.convivir.integrantes;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResidenteAutorizado extends Integrante {

    private String parentesco;

    public ResidenteAutorizado(String documento, String nombres, String apellidos, String parentesco) {
        super(documento, nombres, apellidos);
        this.parentesco = parentesco;
    }

    @Override
    public String obtenerRolDelIntegrante() {
        return "RESIDENTE_AUTORIZADO";
    }
}
