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

FROM build AS jre
# Runtime de Java a la medida (módulos de jdeps + los que se cargan por reflexión), como en pdp/Dockerfile.
RUN MODULES="$(jdeps --ignore-missing-deps --multi-release 25 --print-module-deps --recursive \
        -cp '/layers/dependencies/lib/*' /layers/application/app.jar),java.logging,java.management,java.xml,jdk.crypto.ec,jdk.naming.dns,jdk.zipfs" \
 && jlink --add-modules "$MODULES" --strip-debug --no-man-pages --no-header-files --compress=zip-6 --output /javaruntime

FROM ubuntu:24.04 AS runtime
ENV JAVA_HOME=/opt/java PATH=/opt/java/bin:$PATH
RUN groupadd --gid 10001 app \
 && useradd --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app \
 && mkdir -p /app && chown app:app /app
COPY --from=jre /javaruntime $JAVA_HOME
# Suma al almacén de Java los certificados de CA montados en /certs (como la imagen eclipse-temurin).
COPY --chmod=755 docker/java-entrypoint.sh /usr/local/bin/java-entrypoint.sh
WORKDIR /app
COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/spring-boot-loader/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./
USER 10001
EXPOSE 8081
ENTRYPOINT ["/usr/local/bin/java-entrypoint.sh", "-jar", "/app/app.jar"]
