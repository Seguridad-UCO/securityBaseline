# Diseño aprobado: línea base de registro de aplicaciones protegidas

La línea base implementa E-1 / UC-02: registrar una aplicación protegida mínima con nombre, tenant y recurso. La decisión es un modulith reactivo en Java 25 y Spring Boot 4.1.0, con Clean Architecture/hexagonal dentro de `applications`.

El dominio conserva agregado y value objects; aplicación define puertos; infraestructura aporta WebFlux, auditoría y persistencia dummy. La persistencia dummy permite probar flujo y rollback antes de integrar SurrealDB. PEP/PDP/OPA/Keycloak pertenecen a historias posteriores y se conectarán mediante puertos, sin reescribir el dominio.
