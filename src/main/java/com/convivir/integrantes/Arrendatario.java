package com.convivir.integrantes;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Arrendatario extends Integrante {

    private String numeroContrato;

    public Arrendatario(String documento, String nombres, String apellidos, String numeroContrato) {
        super(documento, nombres, apellidos);
        this.numeroContrato = numeroContrato;
    }

    @Override
    public String obtenerRolDelIntegrante() {
        return "ARRENDATARIO";
    }
}
