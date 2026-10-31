# Multi-stage build: compile with Maven, run with Java 25 runtime.
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
# El jar se descompone en capas (dependencias / aplicación): la imagen pesa lo mismo, pero quien ya la tiene
# descarga solo la capa de la aplicación (~2 MB) cuando cambia el código, no las ~85 MB de dependencias.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -f pdp/pom.xml -B -DskipTests package \
 && java -Djarmode=tools -jar pdp/target/*.jar extract --layers --destination /layers \
 && mv /layers/application/*.jar /layers/application/app.jar

# Runtime stage: Java 25 runtime
FROM eclipse-temurin:25-jre
WORKDIR /app
# De la capa que menos cambia a la que más: dependencias, cargador, snapshots, aplicación.
COPY --from=builder /layers/dependencies/ ./
COPY --from=builder /layers/spring-boot-loader/ ./
COPY --from=builder /layers/snapshot-dependencies/ ./
COPY --from=builder /layers/application/ ./

# La imagen JRE no trae curl; bash sí, y /dev/tcp alcanza (misma sonda que usan los compose).
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health HTTP/1.1\\r\\nHost: localhost\\r\\nConnection: close\\r\\n\\r\\n' >&3 && grep -q '\"status\":\"UP\"' <&3"]

# Run the application
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
