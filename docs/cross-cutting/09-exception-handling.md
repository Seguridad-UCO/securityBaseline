# 09. Manejo de excepciones

[← Observabilidad](08-logging-instrumentation.md) · [Siguiente: primero arquitectura →](23-architecture-first.md)

## Decisión arquitectónica

Cada condición de error semánticamente distinta tiene su propia excepción, con su propio código
estable y su propio mensaje. La traducción a HTTP ocurre una sola vez, en el adaptador de entrada.

## Justificación

Una excepción genérica obliga al cliente —y a quien depura— a leer el texto del mensaje para saber
qué pasó. Con una clase por condición, el motivo es un tipo y no una cadena. `try/catch` dispersos
ocultan causas y producen respuestas distintas para el mismo problema; se descarta también que el
dominio devuelva códigos HTTP o `ResponseEntity`.

## Implementación

Dos jerarquías separadas, porque son dos preguntas distintas:

```text
RuntimeException
├── DomainException                      el núcleo rechaza la operación
│   ├── InvalidValueException            un value object no puede representar el valor
│   │   ├── InvalidTenantIdException
│   │   ├── InvalidApplicationNameException
│   │   ├── InvalidIdentifierException
│   │   ├── InvalidPageWindowException
│   │   ├── InvalidResourceCodeException
│   │   └── InvalidActionCodeException
│   └── BusinessRuleViolationException   una regla dijo que no
│       ├── ConflictBusinessRuleException  conflicto de unicidad → HTTP 409
│       │   ├── DuplicateApplicationException
│       │   └── DuplicateProtectedResourceException
│       ├── TenantNotFoundException
│       ├── TenantNotActiveException
│       ├── ReservedApplicationNameException
│       └── ResourceTenantMismatchException
└── RequestContractException             la petición no honra el contrato HTTP
    ├── MissingRequestFieldException
    ├── MalformedRequestFieldException
    └── ConflictingRequestParametersException
```

`RequestContractException` **no** extiende `DomainException` a propósito: la forma de lo que llegó
por HTTP es algo que el núcleo no conoce ni debe aprender. Mantenerlas separadas es lo que permite
responder de forma distinta a “tu petición está mal formada” y a “tu petición es válida pero está
prohibida”.

Los textos orientados al usuario viven en catálogos del módulo OPEN
[`crosscutting/messages`](../../src/main/java/co/edu/uco/seguridad/pdp/commons/message)
(`ValueObjectMessages`, `ApplicationsMessages`, `ResourcesMessages`, `TenantsMessages`,
`WebContractMessages`). Las excepciones no contienen literales en español.

`ApiErrorHandler` traduce `ConflictBusinessRuleException` a 409 mediante `instanceof`, sin importar
excepciones concretas de cada módulo.

Cada regla lanza exactamente una excepción, y cada excepción la lanza exactamente una regla. Los
casos que antes compartían tipo y ahora no:

- `TenantUnavailableException` se dividió en `TenantNotFoundException` y `TenantNotActiveException`.
  Un tenant desconocido es una petición equivocada; uno suspendido es una decisión temporal de
  política. El cliente hace cosas distintas con cada uno.
- Un nombre duplicado y un nombre reservado ya no comparten tipo: el primero deja de aplicar cuando
  el otro registro desaparece, el segundo nunca deja de aplicar.

## Ubicación verificable

- Bases: [`DomainException.java`](../../src/main/java/co/edu/uco/seguridad/pdp/commons/exception/DomainException.java),
  [`InvalidValueException.java`](../../src/main/java/co/edu/uco/seguridad/pdp/commons/exception/InvalidValueException.java),
  [`BusinessRuleViolationException.java`](../../src/main/java/co/edu/uco/seguridad/pdp/commons/exception/BusinessRuleViolationException.java),
  [`ConflictBusinessRuleException.java`](../../src/main/java/co/edu/uco/seguridad/pdp/commons/exception/ConflictBusinessRuleException.java)
- Frontera: [`RequestContractException.java`](../../src/main/java/co/edu/uco/seguridad/shared/web/exception/RequestContractException.java)
- Traducción: [`ApiErrorHandler.java`](../../src/main/java/co/edu/uco/seguridad/shared/web/exceptionhandler/ApiErrorHandler.java)
- Mensajes: [`crosscutting/messages`](../../src/main/java/co/edu/uco/seguridad/pdp/commons/message)

## Evidencia y límite

Cada excepción se prueba mediante el escenario que la genera, no construyéndola directamente: las
pruebas de reglas la provocan y la prueba HTTP comprueba el código que llega al cliente. El
`ApiErrorHandler` incluye un handler de último recurso para `Exception` que registra la causa y
devuelve un 500 sin datos técnicos.
