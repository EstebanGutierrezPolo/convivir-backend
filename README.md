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

## Comprobación

`GET /api/estado` responde si la API está activa y si llega a la base de datos.
