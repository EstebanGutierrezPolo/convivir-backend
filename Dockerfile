# Etapa 1: compila con Maven y JDK 21 dentro del contenedor (el PC no necesita Java).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# Etapa 2: imagen final, solo con el JRE y el .jar.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/convivir.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
