# Alineación ejecutable con el PDP de referencia

[← Arquitectura](README.md)

## Estructura implementada

La línea base representa el **contenedor PDP**, no un módulo genérico de aplicaciones:

```text
co.edu.uco.seguridad
├── shared/            capacidades transversales, fuera del análisis de módulos
│   ├── rule/          BusinessRule · ReactiveBusinessRule · ReactiveBusinessRuleWithResult
│   ├── port/          TimeProvider · IdentifierGenerator · ReactiveTransactionPort
│   ├── web/           ApiResponse · PageResponse · correlación · excepciones de contrato
│   ├── observability/ ReactiveLogContext
│   └── config/        implementaciones por defecto de los puertos transversales
└── pdp/
    ├── commons/       shared kernel: TenantId, ApplicationId, ResourceId, ApplicationName,
    │                  PageWindow, ResultPage y las excepciones base del dominio
    ├── tenants/       API TenantModuleApi + regla publicada TenantMustBeActiveRule
    ├── aplicaciones/  API ApplicationsModuleApi; registro, unicidad y nombres reservados
    └── recursos/      APIs de registro y consulta del catálogo; orquesta E-1
```

Esto implementa el fragmento aplicable del dependency map de referencia:
`aplicaciones → commons, tenants` y `recursos → commons, tenants, aplicaciones`. Las APIs públicas
viven en la raíz de cada módulo; `domain`, `application` e `infrastructure` son internos y Modulith
lo verifica.

`shared` está fuera del paquete base del análisis (`co.edu.uco.seguridad.pdp`) a propósito: son
capacidades técnicas disponibles para cualquier adaptador, no vocabulario del PDP. El vocabulario
del negocio vive en `pdp/commons`, que sí es un módulo y sí es Java puro sin Reactor.

## Contratos publicados por módulo

| Módulo | Publica | No publica |
|---|---|---|
| `tenants` | `TenantModuleApi`, `TenantMustBeActiveRule`, `TenantSnapshot`, `TenantStatus`, sus dos excepciones | `Tenant`, `TenantRepository`, adaptadores |
| `aplicaciones` | `ApplicationsModuleApi`, comando, resultado, sus dos excepciones | `Application`, `ApplicationRepository`, reglas, adaptadores |
| `recursos` | casos de uso, comando, query, entrada de catálogo, sus dos excepciones | dominio, reglas, puertos, adaptadores |

La separación entre `TenantModuleApi` y `TenantMustBeActiveRule` es intencional: la API responde
*qué* es un tenant y la regla decide *si* puede operar. Un único método que hiciera ambas cosas
obligaría a cada consumidor a interpretar un `Mono` vacío como una decisión de negocio.

## Flujo E-1

El adaptador HTTP de `recursos` mapea el JSON crudo a un DTO validado y llama al interactor. El caso
de uso registra la aplicación con `ApplicationsModuleApi` —que aplica sus propias reglas, incluida
la del tenant— y luego valida y persiste el recurso con su código y acción explícitos. Los
identificadores `estudiantes` y `consultar` sustituyen a la ruta HTTP como fuente de verdad.

## Evidencia

- Arranque: [`PdpApplication.java`](../../src/main/java/co/edu/uco/seguridad/pdp/PdpApplication.java)
- Gate: [`ModulithStructureTests.java`](../../src/test/java/co/edu/uco/seguridad/ModulithStructureTests.java)
- Flujo HTTP: [`ProtectedApplicationHttpTests.java`](../../src/test/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/ProtectedApplicationHttpTests.java)

`./mvnw verify` comprueba compilación, contexto Spring, dependencias Modulith, 109 pruebas y
cobertura.
