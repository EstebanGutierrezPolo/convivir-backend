package com.convivir.inmuebles;

import com.convivir.comun.ReglaDeNegocioException;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TipoInmueble {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String nombre;

    private String descripcion;
    private boolean esBienComun;
    private boolean permiteCesion;
    private boolean permiteReserva;
    private boolean generaCuota;
    private double valorTarifa;

    @Enumerated(EnumType.STRING)
    private UnidadTarifa unidadTarifa;

    private double valorMulta;

    @Enumerated(EnumType.STRING)
    private EstadoRegistro estado;

    public TipoInmueble(String nombre, boolean esBienComun) {
        this.nombre = nombre == null ? null : nombre.trim();
        this.esBienComun = esBienComun;
    }

    /** RF-112. Deja el tipo listo para usarse en inmuebles. */
    public void registrarTipoDeInmueble() {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("Escribe el nombre del tipo de inmueble.");
        }
        validarCondiciones();
        this.estado = EstadoRegistro.ACTIVO;
    }

    /** RF-113. Cambia si se cede, si se reserva, si paga cuota y su tarifa. */
    public void modificarParametrosDelTipoDeInmueble(String descripcion, boolean permiteCesion,
            boolean permiteReserva, boolean generaCuota, double valorTarifa, UnidadTarifa unidadTarifa,
            double valorMulta) {
        this.descripcion = descripcion;
        this.permiteCesion = permiteCesion;
        this.permiteReserva = permiteReserva;
        this.generaCuota = generaCuota;
        this.valorTarifa = permiteReserva ? valorTarifa : 0;
        this.unidadTarifa = permiteReserva ? unidadTarifa : null;
        this.valorMulta = permiteReserva ? valorMulta : 0;
        validarCondiciones();
    }

    /** RF-114. El servicio revisa antes que ningún inmueble activo dependa de él. */
    public void desactivarTipoDeInmueble() {
        if (estado == EstadoRegistro.INACTIVO) {
            throw new ReglaDeNegocioException("El tipo " + nombre + " ya está desactivado.");
        }
        this.estado = EstadoRegistro.INACTIVO;
    }

    private void validarCondiciones() {
        if (permiteReserva && unidadTarifa == null) {
            throw new ReglaDeNegocioException("Si el tipo se reserva, elige si la tarifa es por hora, por día o por evento.");
        }
        if (valorTarifa < 0 || valorMulta < 0) {
            throw new ReglaDeNegocioException("La tarifa y la multa no pueden ser negativas.");
        }
    }
}
