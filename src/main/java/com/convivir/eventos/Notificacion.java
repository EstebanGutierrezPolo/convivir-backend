package com.convivir.eventos;

import java.time.LocalDateTime;

import com.convivir.integrantes.Integrante;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDateTime fechaHora;

    @Column(length = 1000)
    private String contenido;

    @Enumerated(EnumType.STRING)
    private ResultadoEnvio resultadoEnvio;

    // ConfiguracionNotificacion "1" --> "*" Notificacion : origina
    @ManyToOne
    private ConfiguracionNotificacion configuracionNotificacion;

    // Integrante "1" --> "*" Notificacion : recibe (vacío si el destinatario es un correo externo)
    @ManyToOne
    private Integrante destinatario;

    public Notificacion(String contenido) {
        this.contenido = contenido;
    }

    /**
     * Decisión 9: el servicio de notificaciones es externo y todavía no existe.
     * Mientras tanto el envío se simula escribiendo en el registro del backend.
     */
    public void enviarNotificacionAlDestinatario(ConfiguracionNotificacion configuracion, Integrante destinatario) {
        this.configuracionNotificacion = configuracion;
        this.destinatario = destinatario;
        this.fechaHora = LocalDateTime.now();
        log.info("Notificación a {}: {}", configuracion.getDestinatario(), contenido);
        registrarResultadoDelEnvio(ResultadoEnvio.ENVIADA);
    }

    public void registrarResultadoDelEnvio(ResultadoEnvio resultadoEnvio) {
        this.resultadoEnvio = resultadoEnvio;
    }
}
