package com.convivir.eventos;

import java.util.ArrayList;
import java.util.List;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.inmuebles.PropiedadHorizontal;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfiguracionNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String evento;

    private String destinatario;
    private boolean estaHabilitada;

    // PropiedadHorizontal "1" *-- "*" ConfiguracionNotificacion : establece
    @ManyToOne(optional = false)
    private PropiedadHorizontal propiedadHorizontal;

    // ConfiguracionNotificacion "1" --> "*" Notificacion : origina
    @OneToMany(mappedBy = "configuracionNotificacion")
    private List<Notificacion> notificaciones = new ArrayList<>();

    public ConfiguracionNotificacion(String evento, String destinatario) {
        this.evento = evento;
        this.destinatario = destinatario;
    }

    /** CU-17. Define a quién avisa este evento y si está habilitado. */
    public void configurarEventoDeNotificacion(PropiedadHorizontal propiedadHorizontal, String destinatario,
            boolean estaHabilitada) {
        if (destinatario == null || destinatario.isBlank()) {
            throw new ReglaDeNegocioException("Escribe a quién se le avisa este evento.");
        }
        this.propiedadHorizontal = propiedadHorizontal;
        this.destinatario = destinatario.trim();
        this.estaHabilitada = estaHabilitada;
    }
}
