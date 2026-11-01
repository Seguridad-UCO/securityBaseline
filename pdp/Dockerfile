# Multi-stage build: compila con Maven, recorta el runtime de Java con jlink y ejecuta sin root.
#
# Contexto de build = raiz del repositorio (igual que pep/Dockerfile), porque el wrapper de Maven
# (mvnw, .mvn) es compartido entre pdp/ y pep/ y vive un nivel arriba de este pom. Invocar:
#   docker build -f pdp/Dockerfile -t pdp:local .
#
# Orden de capas (de lo más estable a lo que más cambia): imagen base -> wrapper de Maven -> pom.xml y
# dependencias -> código fuente. Cambiar una línea de código solo invalida desde `COPY pdp/src`; las
# dependencias no se vuelven a descargar. La caché de ~/.m2 (BuildKit) evita además descargarlas de nuevo
# cuando cambia el pom.xml. Qué entra al contexto lo fija pdp/Dockerfile.dockerignore.
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /build
COPY mvnw ./
COPY .mvn .mvn
COPY pdp/pom.xml pdp/pom.xml
RUN --mount=type=cache,target=/root/.m2 chmod +x mvnw && ./mvnw -f pdp/pom.xml -B dependency:go-offline
COPY pdp/src pdp/src
# El jar se descompone en capas (dependencias / aplicación): quien ya tiene la imagen descarga solo la capa
# de la aplicación (~2 MB) cuando cambia el código, no las ~85 MB de dependencias.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -f pdp/pom.xml -B -DskipTests package \
 && java -Djarmode=tools -jar pdp/target/*.jar extract --layers --destination /layers \
 && mv /layers/application/*.jar /layers/application/app.jar
# Runtime de Java a la medida: solo los módulos que usan la aplicación y sus dependencias (jdeps, así se
# ajusta solo cuando cambian las dependencias) más los que se cargan por reflexión (TLS, DNS, JMX, zip).
RUN MODULES="$(jdeps --ignore-missing-deps --multi-release 25 --print-module-deps --recursive \
        -cp '/layers/dependencies/lib/*' /layers/application/app.jar),java.logging,java.management,java.xml,jdk.crypto.ec,jdk.naming.dns,jdk.zipfs" \
 && jlink --add-modules "$MODULES" --strip-debug --no-man-pages --no-header-files --compress=zip-6 --output /javaruntime

# Runtime: Ubuntu mínimo + el Java recortado; sin compilador, sin Maven, sin curl.
FROM ubuntu:24.04
ENV JAVA_HOME=/opt/java PATH=/opt/java/bin:$PATH
# Usuario sin privilegios (no root). /app y el directorio de logs le pertenecen para que pueda escribir.
RUN groupadd --gid 10001 app \
 && useradd --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app \
 && mkdir -p /app/observability/logs /var/log/security \
 && chown -R app:app /app /var/log/security
COPY --from=builder /javaruntime $JAVA_HOME
# Suma al almacén de Java los certificados de CA montados en /certs (como la imagen eclipse-temurin).
COPY --chmod=755 docker/java-entrypoint.sh /usr/local/bin/java-entrypoint.sh
WORKDIR /app
# De la capa que menos cambia a la que más: dependencias, cargador, snapshots, aplicación.
COPY --from=builder /layers/dependencies/ ./
COPY --from=builder /layers/spring-boot-loader/ ./
COPY --from=builder /layers/snapshot-dependencies/ ./
COPY --from=builder /layers/application/ ./
USER 10001

# La imagen no trae curl; bash sí, y /dev/tcp alcanza (misma sonda que usan los compose).
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health HTTP/1.1\\r\\nHost: localhost\\r\\nConnection: close\\r\\n\\r\\n' >&3 && grep -q '\"status\":\"UP\"' <&3"]

EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/java-entrypoint.sh", "-Xmx512m", "-jar", "app.jar"]
