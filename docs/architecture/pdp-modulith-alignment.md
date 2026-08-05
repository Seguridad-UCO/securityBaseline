# Alineación ejecutable con el PDP de referencia

## Estructura implementada

La línea base representa el **contenedor PDP**, no un módulo genérico de aplicaciones:

```text
co.edu.uco.seguridad.pdp
├── commons/       shared kernel: TenantId, ApplicationId, ResourceId
├── tenants/       API TenantModuleApi; dummy de tenant activo
├── aplicaciones/  API ApplicationsModuleApi; registro y unicidad
└── recursos/      API RegisterProtectedApplicationUseCase; orquesta E-1
```

Esto implementa exactamente el fragmento aplicable del [dependency map de referencia](../../../security-platform-architecture/docs/03-architecture/modulith-dependency-map.md): `aplicaciones → commons, tenants` y `recursos → commons, tenants, aplicaciones`. Las APIs públicas viven en la raíz del módulo; `domain`, `application` e `infrastructure` son internos.

## Flujo E-1

El adaptador HTTP de `recursos` llama al caso de uso. Este valida el tenant con `TenantModuleApi`, registra la aplicación con `ApplicationsModuleApi` y persiste el recurso con código y acción explícitos. Los identificadores `estudiantes` y `consultar` sustituyen la ruta HTTP como fuente de verdad, coherente con la aplicación de validación `gestion-academica`.

## Evidencia

- Arranque: [`PdpApplication.java`](../../src/main/java/co/edu/uco/seguridad/pdp/PdpApplication.java).
- Gate: [`ModulithStructureTests.java`](../../src/test/java/co/edu/uco/seguridad/ModulithStructureTests.java).
- Flujo HTTP: [`ProtectedApplicationHttpTests.java`](../../src/test/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/web/ProtectedApplicationHttpTests.java).

`./mvnw test` ejecutado con Java 25 verifica compilación, contexto Spring, dependencias Modulith y endpoint WebFlux en Netty.
