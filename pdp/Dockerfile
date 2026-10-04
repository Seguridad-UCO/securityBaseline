# Multi-stage build: compile with Maven, run with Java 25 runtime.
#
# Contexto de build = raiz del repositorio (igual que pep/Dockerfile), porque el wrapper de Maven
# (mvnw, .mvn) es compartido entre pdp/ y pep/ y vive un nivel arriba de este pom. Invocar:
#   docker build -f pdp/Dockerfile -t pdp:local .
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /build
COPY mvnw ./
COPY .mvn .mvn
COPY pdp/pom.xml pdp/pom.xml
RUN chmod +x mvnw && ./mvnw -f pdp/pom.xml -B dependency:go-offline
COPY pdp/src pdp/src
RUN ./mvnw -f pdp/pom.xml -B -DskipTests package

# Runtime stage: Java 25 runtime
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN apt-get update && apt-get install --no-install-recommends -y curl && rm -rf /var/lib/apt/lists/*
COPY --from=builder /build/pdp/target/*.jar app.jar

# Health check for Azure App Service
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
