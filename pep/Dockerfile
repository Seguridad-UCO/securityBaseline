FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY mvnw ./
COPY .mvn .mvn
COPY pep/pom.xml pep/pom.xml
RUN chmod +x mvnw && ./mvnw -f pep/pom.xml -B dependency:go-offline
COPY pep/src pep/src
RUN ./mvnw -f pep/pom.xml -B -DskipTests package dependency:copy-dependencies -DincludeScope=test

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
COPY --from=build /workspace/pep/target/security-pep-0.1.0-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
