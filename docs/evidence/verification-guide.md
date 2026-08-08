# Guía de verificación de la línea base

[← Evidencia](README.md)

## Prerrequisito

El POM exige Java 25.

```bash
./mvnw verify
```

`verify` ejecuta compilación, las pruebas y el reporte de cobertura en `target/site/jacoco/`.

## Qué demuestra cada prueba

| Prueba | Criterios | Evidencia |
|---|---|---|
| `ValueObjectTests` | 3, 14, 15, 21 | Invariantes de `TenantId`, `ApplicationName` e identificadores |
| `PageWindowTests` | 18, 19 | Límites de ventana, equivalencia página/rango, inmutabilidad de `ResultPage` |
| `ProtectedResourceDomainTests` | 3, 15, 17, 21 | Formatos kebab-case, partes obligatorias, specification |
| `TenantMustBeActiveRuleImplTests` | 3, 9 | Tenant inexistente frente a suspendido |
| `ApplicationRegistrationRuleTests` | 3, 9, 12 | Regla sin repositorio y regla con repositorio, aisladas |
| `ProtectedResourceRuleTests` | 3, 9, 12 | Consistencia de tenant y unicidad de concesión |
| `RegisterProtectedApplicationUseCaseImplTests` | 2, 7, 10, 11 | Orquestación, rollback y compensación entre módulos |
| `SearchProtectedApplicationsUseCaseImplTests` | 16, 17, 18, 19 | Consulta dinámica, total, páginas estables |
| `RegisterProtectedApplicationRequestMapperTests` | 6, 13, 14 | Raw → validado: válido, ausente, mal formado, fuera de rango |
| `SearchProtectedApplicationsRequestMapperTests` | 6, 17, 18, 19 | Combinaciones de ventana y sus rechazos |
| `ProtectedApplicationInteractorTests` | 11, 13 | El interactor mapea y no decide |
| `ProtectedApplicationHttpTests` | 5, 6, 9, 22 | Flujo completo sobre Netty y contrato de errores |
| `ModulithStructureTests` | 1, 11, 12 | Dependencias de módulo permitidas |

## Demostración manual

```bash
./mvnw spring-boot:run
```

Registro correcto:

```bash
curl -i -X POST localhost:8080/api/v1/protected-applications \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-Id: demo-1' \
  -d '{"tenantId":"universidad-uco","applicationName":"gestion-academica","resourceCode":"estudiantes","action":"consultar"}'
```

Devuelve `201`, código `APPLICATION_REGISTERED` y los identificadores de correlación. Repetir el
mismo POST devuelve `409 APPLICATION_ALREADY_EXISTS`.

Consulta con filtro y rango:

```bash
curl 'localhost:8080/api/v1/protected-applications?tenantId=universidad-uco&nameContains=academica&offset=0&limit=10'
```

### Errores que conviene demostrar

```bash
# Campo ausente -> 400 MISSING_REQUEST_FIELD con "field":"tenantId"
curl -i -X POST localhost:8080/api/v1/protected-applications -H 'Content-Type: application/json' \
  -d '{"applicationName":"gestion-academica","resourceCode":"estudiantes","action":"consultar"}'
```

```bash
# Formato inválido -> 400 MALFORMED_REQUEST_FIELD con "field":"resourceCode"
curl -i -X POST localhost:8080/api/v1/protected-applications -H 'Content-Type: application/json' \
  -d '{"tenantId":"universidad-uco","applicationName":"otra-app","resourceCode":"Estudiantes","action":"consultar"}'
```

```bash
# Nombre reservado -> 400 RESERVED_APPLICATION_NAME
curl -i -X POST localhost:8080/api/v1/protected-applications -H 'Content-Type: application/json' \
  -d '{"tenantId":"universidad-uco","applicationName":"admin","resourceCode":"estudiantes","action":"consultar"}'
```

```bash
# Tenant suspendido -> 400 TENANT_NOT_ACTIVE  (tenant desconocido -> TENANT_NOT_FOUND)
curl -i -X POST localhost:8080/api/v1/protected-applications -H 'Content-Type: application/json' \
  -d '{"tenantId":"colegio-suspendido","applicationName":"app-x","resourceCode":"estudiantes","action":"consultar"}'
```

```bash
# Ventana ambigua -> 400 CONFLICTING_REQUEST_PARAMETERS
curl -i 'localhost:8080/api/v1/protected-applications?offset=10'
```

El punto de estas cuatro últimas: el mensaje que ve el cliente lo produce esta aplicación, con su
código estable y sus identificadores de correlación. Ninguno lo genera el framework antes de que el
código lo vea, que es exactamente lo que la estrategia de DTO en dos niveles vino a resolver.
