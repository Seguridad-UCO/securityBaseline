# Guía de verificación de la línea base

## Prerrequisito

El POM exige Java 25. Instalar/seleccionar JDK 25 y confirmar `java -version` antes de validar el artefacto objetivo.

## Ejecución automatizada

```bash
cd securityBaseline
./mvnw test
```

Las pruebas relevantes son:

| Prueba | Evidencia |
|---|---|
| `ProtectedApplicationTests` | value objects, agregado e integridad. |
| `ProtectedApplicationServiceTests` | contratos, unicidad y rollback dummy. |
| `ModulithStructureTests` | estructura de módulos. |
| `ProtectedApplicationHttpTests` | WebFlux/Netty, API, correlación, filtros y rangos. |

## Demostración manual

```bash
./mvnw spring-boot:run
curl -i -X POST localhost:8080/api/v1/protected-applications \
  -H 'Content-Type: application/json' \
  -H 'X-Request-Id: demo-1' \
  -H 'X-Correlation-Id: demo-1' \
  -d '{"name":"Billing API","tenantId":"tenant-a","resource":"/invoices"}'
curl 'localhost:8080/api/v1/protected-applications?tenantId=tenant-a&offset=0&limit=10'
```

Se observa `201`, código `APPLICATION_REGISTERED`, IDs de correlación en respuesta/log y resultado limitado. Repetir el POST demuestra `409 APPLICATION_ALREADY_EXISTS`.
