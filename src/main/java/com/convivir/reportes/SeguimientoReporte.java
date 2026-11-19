package com.convivir.reportes;

import java.time.LocalDateTime;

import com.convivir.comun.ReglaDeNegocioException;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeguimientoReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDateTime fechaHora;

    @Column(length = 2000)
    private String observacion;

    // Reporte "1" *-- "*" SeguimientoReporte : recibe
    @ManyToOne(optional = false)
    private Reporte reporte;

    public SeguimientoReporte(String observacion) {
        this.observacion = observacion == null ? null : observacion.trim();
    }

    /** RF-092. Queda en la línea de tiempo del reporte. */
    public void registrarObservacionDeSeguimiento(Reporte reporte) {
        if (observacion == null || observacion.isBlank()) {
            throw new ReglaDeNegocioException("Escribe la observación.");
        }
        this.reporte = reporte;
        this.fechaHora = LocalDateTime.now();
        reporte.getSeguimientos().add(this);
    }
}
