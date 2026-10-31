# Orden de capas: imagen base -> wrapper de Maven -> pom.xml y dependencias -> código fuente. Cambiar una línea
# de código no vuelve a descargar dependencias (y la caché de ~/.m2 de BuildKit las reutiliza si cambia el pom).
# Qué entra al contexto lo fija pep/Dockerfile.dockerignore (el contexto es la raíz del repositorio).
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY mvnw ./
COPY .mvn .mvn
COPY pep/pom.xml pep/pom.xml
RUN --mount=type=cache,target=/root/.m2 chmod +x mvnw && ./mvnw -f pep/pom.xml -B dependency:go-offline
COPY pep/src pep/src
# Además del paquete: las dependencias de prueba para la imagen de fixtures y el jar descompuesto en capas
# (quien ya tiene la imagen descarga solo la capa de la aplicación cuando cambia el código).
RUN --mount=type=cache,target=/root/.m2 ./mvnw -f pep/pom.xml -B -DskipTests package dependency:copy-dependencies -DincludeScope=test \
 && java -Djarmode=tools -jar pep/target/security-pep-0.1.0-SNAPSHOT.jar extract --layers --destination /layers \
 && mv /layers/application/*.jar /layers/application/app.jar

# Test-only target. The runtime stage never copies fixtures or test dependencies.
FROM eclipse-temurin:25-jre AS fixtures
WORKDIR /fixtures
COPY --from=build /workspace/pep/target/test-classes test-classes
COPY --from=build /workspace/pep/target/classes classes
COPY --from=build /workspace/pep/target/dependency dependency
USER 10001
EXPOSE 18080 18081
ENTRYPOINT ["java", "-Dfixture.bind-host=0.0.0.0", "-cp", "test-classes:classes:dependency/*", "co.edu.uco.seguridad.pep.fixture.FixtureServers"]

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/spring-boot-loader/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./
USER 10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
