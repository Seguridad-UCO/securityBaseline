# Gobierno arquitectónico

[← Índice principal](../README.md)

Registro de decisiones arquitectónicas (ADR) de la línea base. Un ADR registra una decisión ya
tomada, su contexto y sus consecuencias, y se mantiene aunque más adelante se reemplace (un ADR
superado se marca como tal, no se borra). La hoja de ruta por etapas está en
[arquitectura y hoja de ruta](../plans/2026-08-07-architecture-and-roadmap.md).

## Índice

| ADR | Título | Estado |
|---|---|---|
| [0001](adr/adr-0001-keep-interactor-layer.md) | Conservar la capa de interactor | Aceptada e implementada |
| [0002](adr/adr-0002-domain-events-modulith-registry.md) | Adoptar eventos de dominio vía ApplicationEventPublisher | Implementada |
| [0003](adr/adr-0003-real-security-reactive-jwt.md) | Seguridad real con Spring Security reactivo y JWT | Implementada |
| [0004](adr/adr-0004-real-persistence-surrealdb.md) | Persistencia real con SurrealDB detrás de los puertos existentes | Implementada |
