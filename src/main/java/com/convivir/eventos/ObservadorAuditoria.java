package com.convivir.eventos;

import org.springframework.stereotype.Component;

import com.convivir.inmuebles.PropiedadHorizontal;
import com.convivir.inmuebles.PropiedadHorizontalRepositorio;
import com.convivir.integrantes.Integrante;
import com.convivir.integrantes.IntegranteRepositorio;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * RN-30. Cada evento del dominio queda en la auditoría (diagrama de secuencia transversal).
 * Las consultas de aquí no fuerzan el flush (ver los repositorios): el evento se
 * publica en medio de un método del dominio, cuando puede haber objetos sin guardar.
 */
@Component
public class ObservadorAuditoria implements Observador {

    private final AuditoriaRepositorio auditoriaRepositorio;
    private final PropiedadHorizontalRepositorio propiedadHorizontalRepositorio;
    private final IntegranteRepositorio integranteRepositorio;

    public ObservadorAuditoria(AuditoriaRepositorio auditoriaRepositorio,
            PropiedadHorizontalRepositorio propiedadHorizontalRepositorio, IntegranteRepositorio integranteRepositorio) {
        this.auditoriaRepositorio = auditoriaRepositorio;
        this.propiedadHorizontalRepositorio = propiedadHorizontalRepositorio;
        this.integranteRepositorio = integranteRepositorio;
    }

    @PostConstruct
    void suscribirse() {
        PublicadorEventos.obtenerInstancia().suscribir(this);
    }

    @PreDestroy
    void desuscribirse() {
        PublicadorEventos.obtenerInstancia().desuscribir(this);
    }

    @Override
    public void actualizar(EventoSistema evento) {
        Auditoria auditoria = new Auditoria(evento.getTipoEvento(), evento.getElementoAfectado());
        PropiedadHorizontal propiedad = propiedadHorizontalRepositorio.findFirstByOrderByIdAsc().orElse(null);
        Integrante actor = integranteRepositorio.findByDocumento(evento.getDocumentoActor()).orElse(null);
        auditoria.registrarOperacionEnAuditoria(propiedad, actor, evento.getDocumentoActor());
        auditoriaRepositorio.save(auditoria);
    }
}
