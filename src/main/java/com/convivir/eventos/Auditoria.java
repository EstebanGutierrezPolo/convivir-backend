package com.convivir.eventos;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.inmuebles.PropiedadHorizontal;
import com.convivir.integrantes.Integrante;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private String accion;

    private String elementoAfectado;

    /** Documento de quien actuó, también cuando no es un integrante registrado (SISTEMA). */
    private String documentoActor;

    // PropiedadHorizontal "1" --> "*" Auditoria : conserva
    @ManyToOne
    private PropiedadHorizontal propiedadHorizontal;

    // Integrante "1" --> "*" Auditoria : realiza
    @ManyToOne
    private Integrante integrante;

    public Auditoria(String accion, String elementoAfectado) {
        this.accion = accion;
        this.elementoAfectado = elementoAfectado;
    }

    /** RN-30. Toda operación queda con su fecha, quién la hizo y sobre qué. */
    public void registrarOperacionEnAuditoria(PropiedadHorizontal propiedadHorizontal, Integrante integrante,
            String documentoActor) {
        this.fechaHora = LocalDateTime.now();
        this.propiedadHorizontal = propiedadHorizontal;
        this.integrante = integrante;
        this.documentoActor = documentoActor;
    }

    /** RF-109. Devuelve en CSV los registros entre dos fechas, incluidas. */
    public static byte[] exportarRegistroDeAuditoria(List<Auditoria> registros, LocalDate fechaDesde,
            LocalDate fechaHasta) {
        if (fechaDesde.isAfter(fechaHasta)) {
            throw new ReglaDeNegocioException("La fecha inicial va antes que la final.");
        }
        String filas = registros.stream()
                .filter(a -> !a.fechaHora.toLocalDate().isBefore(fechaDesde)
                        && !a.fechaHora.toLocalDate().isAfter(fechaHasta))
                .map(a -> String.join(";", a.fechaHora.toString(), a.documentoActor, a.accion,
                        a.elementoAfectado == null ? "" : a.elementoAfectado))
                .collect(Collectors.joining("\n"));
        return ("fechaHora;documentoActor;accion;elementoAfectado\n" + filas).getBytes(StandardCharsets.UTF_8);
    }
}
