package com.convivir.inmuebles;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.convivir.comun.ConflictoException;
import com.convivir.comun.NoEncontradoException;
import com.convivir.comun.ReglaDeNegocioException;
import com.convivir.inmuebles.InmuebleDatos.*;

/**
 * CU-02, siguiendo su diagrama de secuencia. El servicio coordina: busca,
 * llama al método de Inmueble y guarda. Las reglas están en Inmueble.
 */
@Service
@Transactional
public class InmuebleServicio {

    private final InmuebleRepositorio inmuebleRepositorio;
    private final TipoInmuebleRepositorio tipoInmuebleRepositorio;
    private final PropiedadHorizontalRepositorio propiedadHorizontalRepositorio;

    public InmuebleServicio(InmuebleRepositorio inmuebleRepositorio, TipoInmuebleRepositorio tipoInmuebleRepositorio,
            PropiedadHorizontalRepositorio propiedadHorizontalRepositorio) {
        this.inmuebleRepositorio = inmuebleRepositorio;
        this.tipoInmuebleRepositorio = tipoInmuebleRepositorio;
        this.propiedadHorizontalRepositorio = propiedadHorizontalRepositorio;
    }

    @Transactional(readOnly = true)
    public List<Respuesta> consultarInmuebles() {
        return inmuebleRepositorio.findAllByOrderByIdentificacionAsc().stream().map(Respuesta::de).toList();
    }

    /** RF-006. */
    public Respuesta registrarInmueble(Solicitud datos) {
        TipoInmueble tipo = tipoInmuebleRepositorio.findById(datos.tipoInmuebleId())
                .orElseThrow(() -> new NoEncontradoException("No existe el tipo de inmueble " + datos.tipoInmuebleId() + "."));
        Inmueble contenedor = datos.inmuebleContenedorId() == null ? null : buscar(datos.inmuebleContenedorId());
        String identificacion = datos.identificacion().trim();
        if (inmuebleRepositorio.existsByIdentificacionIgnoreCase(identificacion)) {
            throw new ConflictoException("Ya existe un inmueble con la identificación " + identificacion + ".");
        }
        Inmueble inmueble = new Inmueble(identificacion, datos.coeficiente(), datos.areaM2(),
                datos.naturalezaInmueble(), tipo, contenedor);
        PropiedadHorizontal copropiedad = propiedadHorizontalRepositorio.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ReglaDeNegocioException("Primero configura los datos de la copropiedad."));
        inmueble.asignarPropiedadHorizontal(copropiedad);
        inmueble.registrarInmueble();
        return Respuesta.de(inmuebleRepositorio.save(inmueble));
    }

    /** RF-010. */
    public Respuesta modificarInmueble(Integer id, Modificacion datos) {
        Inmueble inmueble = buscar(id);
        inmueble.modificarDatosDelInmueble(datos.coeficiente(), datos.areaM2(), datos.naturalezaInmueble());
        return Respuesta.de(inmuebleRepositorio.save(inmueble));
    }

    /** RF-011, RN-04. El historial se conserva. */
    public Respuesta desactivarInmueble(Integer id, String motivo) {
        Inmueble inmueble = buscar(id);
        inmueble.desactivarInmueblePorMotivo(motivo);
        return Respuesta.de(inmuebleRepositorio.save(inmueble));
    }

    /** RF-012. */
    public Respuesta reactivarInmueble(Integer id) {
        Inmueble inmueble = buscar(id);
        inmueble.reactivarInmuebleDesactivado();
        return Respuesta.de(inmuebleRepositorio.save(inmueble));
    }

    /** RF-013. */
    @Transactional(readOnly = true)
    public Historial consultarHistorial(Integer id) {
        Inmueble i = buscar(id);
        return new Historial(Respuesta.de(i),
                i.consultarHistorialDeVinculacionesDelInmueble().stream()
                        .map(v -> new VinculacionResumen(v.getId(), v.getIntegrante().obtenerNombreCompleto(),
                                v.getIntegrante().obtenerRolDelIntegrante(), v.isEsResidente(), v.getFechaInicio(),
                                v.getFechaFin(), v.getEstado().name(), v.isAutorizaAcceso()))
                        .toList(),
                i.consultarHistorialDeObligacionesDelInmueble().stream()
                        .map(o -> new ObligacionResumen(o.getId(), o.getConcepto().name(), o.getDescripcion(), o.getValor(),
                                o.getSaldoPendiente(), o.getFechaVencimiento(), o.getEstado().name(), o.calcularDiasDeMora()))
                        .toList(),
                i.consultarHistorialDePagosDelInmueble().stream()
                        .map(p -> new PagoResumen(p.getId(), p.getFecha(), p.getValor(), p.getMedioPago().name(),
                                p.getCanalRecaudo().name(), p.getComprobante(), p.getEstado().name()))
                        .toList(),
                i.consultarHistorialDeReportesDelInmueble().stream()
                        .map(r -> new ReporteResumen(r.getId(), r.getFechaCreacion(), r.getCategoria().getNombre(),
                                r.getEstado().name()))
                        .toList(),
                i.consultarHistorialDeMovimientosDelInmueble().stream()
                        .map(m -> new MovimientoResumen(m.getId(), m.getFechaHora(), m.getSentidoMovimiento().name(),
                                m.getPlacaVisitante()))
                        .toList());
    }

    Inmueble buscar(Integer id) {
        return inmuebleRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el inmueble " + id + "."));
    }
}
