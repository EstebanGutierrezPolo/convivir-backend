package com.convivir.comun;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.convivir.inmuebles.PropiedadHorizontal;
import com.convivir.inmuebles.PropiedadHorizontalRepositorio;
import com.convivir.inmuebles.TipoInmueble;
import com.convivir.inmuebles.TipoInmuebleRepositorio;
import com.convivir.inmuebles.UnidadTarifa;

/**
 * Deja la base lista para usar la primera vez: la copropiedad y los tipos de
 * inmueble básicos. No hace nada si ya existen. Los datos son de ejemplo.
 */
@Component
@Order(1)
public class DatosIniciales implements ApplicationRunner {

    private final PropiedadHorizontalRepositorio propiedadHorizontalRepositorio;
    private final TipoInmuebleRepositorio tipoInmuebleRepositorio;

    public DatosIniciales(PropiedadHorizontalRepositorio propiedadHorizontalRepositorio,
            TipoInmuebleRepositorio tipoInmuebleRepositorio) {
        this.propiedadHorizontalRepositorio = propiedadHorizontalRepositorio;
        this.tipoInmuebleRepositorio = tipoInmuebleRepositorio;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (propiedadHorizontalRepositorio.count() == 0) {
            PropiedadHorizontal copropiedad = new PropiedadHorizontal("900123456-7", "Villa Campestre",
                    "Calle 98 # 52-115, Barranquilla");
            propiedadHorizontalRepositorio.save(copropiedad);
        }
        if (tipoInmuebleRepositorio.count() == 0) {
            tipo("Apartamento", "Unidad privada de vivienda", false, false, false, true, 0, null, 0);
            tipo("Parqueadero", "Parqueadero privado o de visitantes", false, true, false, true, 0, null, 0);
            tipo("Depósito", "Cuarto útil", false, true, false, true, 0, null, 0);
            tipo("Torre", "Edificio que contiene apartamentos", true, false, false, false, 0, null, 0);
            tipo("Salón social", "Zona común que se reserva por evento", true, false, true, false, 180000, UnidadTarifa.POR_EVENTO, 90000);
            tipo("Zona BBQ", "Zona común que se reserva por hora", true, false, true, false, 25000, UnidadTarifa.POR_HORA, 50000);
        }
    }

    private void tipo(String nombre, String descripcion, boolean esBienComun, boolean permiteCesion,
            boolean permiteReserva, boolean generaCuota, double tarifa, UnidadTarifa unidad, double multa) {
        TipoInmueble tipo = new TipoInmueble(nombre, esBienComun);
        tipo.modificarParametrosDelTipoDeInmueble(descripcion, permiteCesion, permiteReserva, generaCuota, tarifa, unidad, multa);
        tipo.registrarTipoDeInmueble();
        tipoInmuebleRepositorio.save(tipo);
    }
}
