package com.convivir.cesiones;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.convivir.accesos.MovimientoAcceso;
import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.comun.SesionActual;
import com.convivir.eventos.EventoSistema;
import com.convivir.eventos.PublicadorEventos;
import com.convivir.inmuebles.EstadoRegistro;
import com.convivir.inmuebles.Inmueble;
import com.convivir.integrantes.Integrante;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String placa;

    private String tipoVehiculo;
    private String marca;
    private String color;

    @Enumerated(EnumType.STRING)
    private EstadoRegistro estado;

    private String motivoBaja;

    // Inmueble "1" o-- "*" Vehiculo : posee
    @ManyToOne(optional = false)
    private Inmueble inmueble;

    // Integrante "0..1" --> "*" Vehiculo : responde por
    @ManyToOne
    private Integrante responsable;

    // Vehiculo "0..1" --> "*" MovimientoAcceso : genera
    @OneToMany(mappedBy = "vehiculo")
    private List<MovimientoAcceso> movimientos = new ArrayList<>();

    public Vehiculo(String placa, String tipoVehiculo, String marca, String color) {
        this.placa = normalizarPlaca(placa);
        this.tipoVehiculo = tipoVehiculo;
        this.marca = marca;
        this.color = color;
    }

    /** RF-046, RF-047. */
    public void registrarVehiculo(Inmueble inmueble, Integrante responsable) {
        if (placa == null || !placa.matches("[A-Z]{3} \\d{2}[A-Z0-9]")) {
            throw new ReglaDeNegocioException("Escribe la placa con tres letras y tres caracteres, por ejemplo BQA 47F.");
        }
        if (inmueble.getEstado() != EstadoRegistro.ACTIVO) {
            throw new ReglaDeNegocioException("El inmueble " + inmueble.getIdentificacion() + " está desactivado.");
        }
        this.inmueble = inmueble;
        this.responsable = responsable;
        this.estado = EstadoRegistro.ACTIVO;
        inmueble.getVehiculos().add(this);
        publicar("VEHICULO_REGISTRADO");
    }

    /** RF-049. */
    public void modificarDatosDelVehiculo(String tipoVehiculo, String marca, String color) {
        this.tipoVehiculo = tipoVehiculo;
        this.marca = marca;
        this.color = color;
        publicar("VEHICULO_MODIFICADO");
    }

    /** RF-050. */
    public void darDeBajaVehiculoPorMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException("Escribe el motivo de la baja del vehículo.");
        }
        if (estado == EstadoRegistro.INACTIVO) {
            throw new ReglaDeNegocioException("El vehículo " + placa + " ya está dado de baja.");
        }
        this.estado = EstadoRegistro.INACTIVO;
        this.motivoBaja = motivo.trim();
        publicar("VEHICULO_DADO_DE_BAJA");
    }

    /** BQA47F, bqa-47f y BQA 47F quedan iguales: BQA 47F. */
    public static String normalizarPlaca(String placa) {
        if (placa == null) {
            return null;
        }
        String limpia = placa.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return limpia.length() > 3 ? limpia.substring(0, 3) + " " + limpia.substring(3) : limpia;
    }

    private void publicar(String tipoEvento) {
        PublicadorEventos.obtenerInstancia().publicar(new EventoSistema(tipoEvento, "Vehículo " + placa,
                SesionActual.documentoDelActor()));
    }
}
