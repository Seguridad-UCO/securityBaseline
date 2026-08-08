# Diagramas C4

[← Arquitectura](../README.md) · [Gobierno (ADR)](../../governance/README.md)

Dos niveles C4 (Contexto y Contenedor), cada uno en dos versiones: **estado actual** (lo que corre
hoy: autenticación real con emisor propio, persistencia real en SurrealDB) y **evolución prevista**
(lo que resulta de reemplazar el emisor propio por Keycloak y, si llega a existir, un consumidor
externo de auditoría). Los componentes nuevos se marcan explícitamente; nada del estado actual se
elimina de golpe, se sustituye por etapa (ver
[arquitectura y hoja de ruta](../../plans/2026-08-07-architecture-and-roadmap.md)).

## Nivel 1 — Contexto (estado actual)

```mermaid
flowchart TB
    ADMIN["Administrador de aplicaciones\n(persona)"]
    PDP(("Contenedor PDP\nsecurityBaseline"))
    DB[("SurrealDB")]

    ADMIN -->|"HTTP JSON + Bearer JWT\nregistra / consulta catálogo"| PDP
    PDP -->|"valida el JWT él mismo\n(emisor propio, HMAC — ADR-0003)"| PDP
    PDP -->|"persiste el catálogo por HTTP\n(sin driver Java — ADR-0004)"| DB

    classDef person fill:#1d3557,stroke:#0d1b2a,color:#fff
    classDef system fill:#2d6a4f,stroke:#081c15,color:#fff
    classDef ext fill:#6c584c,stroke:#25171a,color:#fff
    class ADMIN person
    class PDP system
    class DB ext
```

El contenedor PDP ya exige y valida autenticación (ADR-0003) y ya persiste en una base de datos real
(ADR-0004), pero todavía sin un proveedor de identidad externo: el propio contenedor firma y valida
sus JWT con una clave compartida. La auditoría sigue detrás de un puerto con implementación dummy en
memoria (solo identificadores, nunca el payload).

## Nivel 1 — Contexto (evolución prevista)

```mermaid
flowchart TB
    ADMIN["Administrador de aplicaciones\n(persona)"]
    PDP(("Contenedor PDP\nsecurityBaseline"))
    IDP["Keycloak\n(emite/valida JWT vía JWKS)"]
    DB[("SurrealDB")]
    AUD["Consumidor externo de auditoría\n(si llega a existir uno)"]

    ADMIN -->|"HTTP JSON + Bearer JWT"| PDP
    PDP -->|"valida token vía JWKS"| IDP
    PDP -->|"persiste catálogo\n(puertos existentes)"| DB
    PDP -.->|"eventos de dominio\n(hoy solo en proceso)"| AUD

    classDef person fill:#1d3557,stroke:#0d1b2a,color:#fff
    classDef system fill:#2d6a4f,stroke:#081c15,color:#fff
    classDef ext fill:#6c584c,stroke:#25171a,color:#fff
    classDef new fill:#7b2cbf,stroke:#3c096c,color:#fff
    class ADMIN person
    class PDP system
    class DB ext
    class IDP,AUD new
```

`IDP` (Keycloak) es evolución prevista, no estado actual: sustituiría el `ReactiveJwtDecoder` HMAC de
hoy sin que el dominio ni los casos de uso cambien (ADR-0003) — el cambio sería
`.withSecretKey(...)` → `.withJwkSetUri(...)` en `SecurityConfiguration`. `DB` (SurrealDB) ya es
estado actual desde ADR-0004, no evolución prevista; se mantiene en este diagrama sin resaltar porque
no cambia entre ambas versiones. `AUD` como *sistema externo* sigue siendo hipotético — hoy la
auditoría es un listener en el mismo proceso (`InMemoryAuditAdapter`), no un sistema aparte; se
dibuja aquí solo para mostrar que el mecanismo de eventos ya lo permitiría el día que uno exista.

## Nivel 2 — Contenedor (estado actual, con lo nuevo desde ADR-0003/ADR-0004 marcado)

```mermaid
flowchart TB
    ADMIN["Administrador\n(persona)"]

    subgraph PDP["Contenedor PDP (Spring Boot / WebFlux / Modulith)"]
        direction TB
        SEC["shared/security\nSecurityWebFilterChain · ReactiveJwtDecoder (HMAC)"]
        WEB["Adaptadores web\n(controllers por módulo)"]
        MODS["Módulos de negocio\ntenants · aplicaciones · recursos"]
        EVT["shared/event\nDomainEventPublisher (ApplicationEventPublisher)"]
        PERSIST["Adaptadores de persistencia\n(SurrealDbClient sobre WebClient,\nsin driver Java — ADR-0004)"]
    end

    AUD["Auditoría\n(listener en el mismo proceso)"]
    DB[("SurrealDB")]

    ADMIN -->|"Bearer JWT"| SEC --> WEB --> MODS
    MODS --> PERSIST --> DB
    MODS -->|"eventos de dominio"| EVT --> AUD

    classDef new fill:#7b2cbf,stroke:#3c096c,color:#fff
    classDef existing fill:#1d3557,stroke:#0d1b2a,color:#fff
    classDef ext fill:#6c584c,stroke:#25171a,color:#fff
    class SEC,PERSIST new
    class WEB,MODS,EVT existing
    class AUD,DB ext
```

`SEC` es lo que añadió la etapa 3: valida el JWT y expone el principal autenticado
(`SecurityContext.currentPrincipal()`) a los interactores de `recursos`, que ya no aceptan el
tenant como parámetro del cuerpo o la query. `PERSIST` es lo que añadió la etapa 4: los tres
repositorios secundarios (`tenants`, `aplicaciones`, `recursos`) hablan con SurrealDB por HTTP a
través de un cliente compartido, sin driver Java de terceros — no había uno viable (ver la
[nota de implementación de ADR-0004](../../governance/adr/adr-0004-real-persistence-surrealdb.md#nota-de-implementación)).

El diagrama de contenedor específico de `securityBaseline` — con los módulos Modulith, sus
dependencias internas y el flujo E-1 completo — está en el
[arquitectura y hoja de ruta](../../plans/2026-08-07-architecture-and-roadmap.md).
Este documento se limita al nivel C4 de contexto y contenedor; el detalle de paquetes vive en
[Estructura PDP / Spring Modulith](../pdp-modulith-alignment.md).
