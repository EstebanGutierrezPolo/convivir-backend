# Convivir · Backend

API REST de Convivir, plataforma para la gestión y control de propiedades horizontales. Hecha con Spring Boot 4 y Java 21, sobre PostgreSQL.

La consume el frontend ([convivir-frontend](https://github.com/EstebanGutierrezPolo/convivir-frontend)).

## Correrlo con Docker

No hace falta tener Java instalado: el `Dockerfile` compila el proyecto con Maven y JDK 21 dentro del contenedor. Se levanta junto a la base de datos y el frontend con el `docker-compose.yml` del proyecto.

## Variables de entorno

| Variable | Para qué |
|---|---|
| `DB_URL` | Dirección JDBC de PostgreSQL, por ejemplo `jdbc:postgresql://db:5432/convivir` |
| `DB_USER` | Usuario de la base |
| `DB_PASSWORD` | Contraseña de la base |
| `CONVIVIR_CONTRASENA_INICIAL` | Contraseña de las cuentas de ejemplo que se crean la primera vez (`paula.ortega`, `jhon.rojas`, `lucia.mendoza`, `hernan.ruiz` y `marta.oviedo`, bloqueada). Si falta, no se crean |

## Cómo está organizado

Un paquete por módulo del diagrama de clases, y en cada uno las clases del dominio con el mismo nombre, atributos y métodos que en el diagrama:

| Paquete | Clases |
|---|---|
| `com.convivir.inmuebles` | `PropiedadHorizontal`, `TipoInmueble`, `Inmueble` |
| `com.convivir.integrantes` | `Integrante` y sus tipos, `FabricaIntegrante`, `Vinculacion`, `Cuenta` |
| `com.convivir.reservas` | `Reserva` con sus estados y tarifas, `AsignacionInmueble`, `ProgramacionAsignacion` |
| `com.convivir.cesiones` | `CesionUso`, `Vehiculo` |
| `com.convivir.accesos` | `AutorizacionAcceso`, `MovimientoAcceso` |
| `com.convivir.finanzas` | `PeriodoFacturacion`, `Obligacion`, `Pago`, `Gasto`, la pasarela y el interés de mora |
| `com.convivir.reportes` | `Reporte`, `CategoriaReporte`, `SeguimientoReporte` |
| `com.convivir.eventos` | `PublicadorEventos`, los observadores, `Auditoria`, `Notificacion` |
| `com.convivir.comun` | Errores de la API, quién hace la petición y datos iniciales. No son del dominio |

La lógica vive en las clases del dominio. Cada caso de uso tiene un `…Controlador`, un `…Servicio` que solo coordina (busca, llama al método de la clase y guarda) y un `…Repositorio`, con los nombres de su diagrama de secuencia.

## API

| Caso de uso | Rutas |
|---|---|
| CU-01 Iniciar sesión | `POST /api/sesion`, `GET /api/sesion`, `DELETE /api/sesion`, `POST /api/sesion/restablecimiento` |
| CU-19 Gestionar tipos de inmueble | `GET /api/tipos-inmueble`, `POST /api/tipos-inmueble`, `PUT /api/tipos-inmueble/{id}`, `PATCH /api/tipos-inmueble/{id}/desactivar` |
| CU-02 Gestionar inmuebles | `GET /api/inmuebles`, `POST /api/inmuebles`, `PUT /api/inmuebles/{id}`, `PATCH /api/inmuebles/{id}/desactivar`, `PATCH /api/inmuebles/{id}/reactivar`, `GET /api/inmuebles/{id}/historial` |

Toda ruta pide sesión (cookie `JSESSIONID`) salvo `/api/estado` y `/api/sesion`. Inmuebles y tipos de inmueble solo los usa el administrador.

Los errores responden con `{"error": "mensaje"}`: 400 si falta un dato, 401 sin sesión, 403 si el rol no tiene acceso, 404 si no existe, 409 si choca con algo que ya existe y 422 si se incumple una regla de negocio.

## Comprobación

`GET /api/estado` responde si la API está activa y si llega a la base de datos.
