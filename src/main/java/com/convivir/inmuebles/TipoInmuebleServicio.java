package com.convivir.inmuebles;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.convivir.comun.ConflictoException;
import com.convivir.comun.NoEncontradoException;

/**
 * CU-19. El servicio solo coordina: busca, llama al método de la clase y guarda.
 * Las reglas están en TipoInmueble.
 */
@Service
@Transactional
public class TipoInmuebleServicio {

    private final TipoInmuebleRepositorio tipoInmuebleRepositorio;
    private final InmuebleRepositorio inmuebleRepositorio;

    public TipoInmuebleServicio(TipoInmuebleRepositorio tipoInmuebleRepositorio,
            InmuebleRepositorio inmuebleRepositorio) {
        this.tipoInmuebleRepositorio = tipoInmuebleRepositorio;
        this.inmuebleRepositorio = inmuebleRepositorio;
    }

    @Transactional(readOnly = true)
    public List<TipoInmuebleDatos.Respuesta> consultarTipos() {
        return tipoInmuebleRepositorio.findAllByOrderByNombreAsc().stream().map(TipoInmuebleDatos.Respuesta::de).toList();
    }

    /** RF-112. */
    public TipoInmuebleDatos.Respuesta registrarTipo(TipoInmuebleDatos.Solicitud datos) {
        if (tipoInmuebleRepositorio.existsByNombreIgnoreCase(datos.nombre().trim())) {
            throw new ConflictoException("Ya existe un tipo de inmueble llamado " + datos.nombre().trim() + ".");
        }
        TipoInmueble tipo = new TipoInmueble(datos.nombre(), datos.esBienComun());
        tipo.modificarParametrosDelTipoDeInmueble(datos.descripcion(), datos.permiteCesion(), datos.permiteReserva(),
                datos.generaCuota(), datos.valorTarifa(), datos.unidadTarifa(), datos.valorMulta());
        tipo.registrarTipoDeInmueble();
        return TipoInmuebleDatos.Respuesta.de(tipoInmuebleRepositorio.save(tipo));
    }

    /** RF-113. */
    public TipoInmuebleDatos.Respuesta modificarTipo(Integer id, TipoInmuebleDatos.Solicitud datos) {
        TipoInmueble tipo = buscar(id);
        tipo.modificarParametrosDelTipoDeInmueble(datos.descripcion(), datos.permiteCesion(), datos.permiteReserva(),
                datos.generaCuota(), datos.valorTarifa(), datos.unidadTarifa(), datos.valorMulta());
        return TipoInmuebleDatos.Respuesta.de(tipoInmuebleRepositorio.save(tipo));
    }

    /** RF-114. */
    public TipoInmuebleDatos.Respuesta desactivarTipo(Integer id) {
        TipoInmueble tipo = buscar(id);
        long activos = inmuebleRepositorio.countByTipoInmuebleAndEstado(tipo, EstadoRegistro.ACTIVO);
        if (activos > 0) {
            throw new ConflictoException("No se puede desactivar " + tipo.getNombre() + ": " + activos
                    + (activos == 1 ? " inmueble activo es" : " inmuebles activos son") + " de este tipo.");
        }
        tipo.desactivarTipoDeInmueble();
        return TipoInmuebleDatos.Respuesta.de(tipoInmuebleRepositorio.save(tipo));
    }

    TipoInmueble buscar(Integer id) {
        return tipoInmuebleRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el tipo de inmueble " + id + "."));
    }
}
