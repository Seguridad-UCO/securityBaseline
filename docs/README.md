# Evidencia arquitectónica de la línea base

Esta documentación sustenta la HU E-1 / UC-02: registrar una aplicación protegida con tenant,
nombre y un recurso con su acción, y consultar el catálogo resultante. Cada criterio solicitado por
el asesor tiene una página que responde qué se decidió, por qué, cómo se implementa y dónde se
verifica.

> **¿Buscas el panorama completo de la plataforma** (PDP + PEP + OPA, diagramas, qué corre hoy y qué
> es plan)? Eso vive en [`PLATAFORMA.md`](PLATAFORMA.md), no aquí — esta página es evidencia de los
> 23 criterios del PDP, un solo componente.

## Navegación por tema

- [Enfoque general de los 23 criterios](baseline-criteria-overview.md): índice para sustentación con
  el estado real de cada criterio.
- [Matriz de cumplimiento](criteria-compliance-matrix.md): estado inicial, problema encontrado,
  cambio realizado y estado final, criterio por criterio.
- [Estructura PDP / Spring Modulith](architecture/pdp-modulith-alignment.md): módulos, contratos
  publicados y flujo E-1.
- [Arquitectura y hoja de ruta](plans/2026-08-07-architecture-and-roadmap.md): objetivo y etapas.
- [Próximos pasos de la Plataforma Central de Seguridad](plans/2026-09-06-security-platform-next-steps.md):
  contraste entre el diagrama C4 objetivo, el PEP implementado y las etapas pendientes para PDP, OPA,
  auditoría y adopción de aplicaciones.
- Gobierno arquitectónico (ADR — interactor, eventos de dominio, seguridad real y persistencia
  real) y diagramas C4 (contexto y contenedor, estado actual y evolución prevista): viven en el
  [repositorio de arquitectura](https://github.com/Seguridad-UCO/security-platform-architecture)
  ([ADR-016](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-016-interactor-layer.md)–[019](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-019-surrealdb-implementation.md),
  [C4](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/03-architecture/c4/README.md)).
- [Arquitectura](architecture/README.md): 1, 2, 11, 12, 20, 21 y 22.
- [Dominio y datos](domain-and-data/README.md): 3, 10 y 15 a 19.
- [Interfaces](interfaces/README.md): 5, 6, 13 y 14.
- [Capacidades transversales](cross-cutting/README.md): 4, 8, 9 y 23.
- [Infraestructura](infrastructure/README.md): 7.
- [Entrega](delivery/README.md): ramas, pipelines, SonarQube y secretos.
- [Evidencia y navegación de código](evidence/README.md).

## Alcance honesto

La línea base implementa dos operaciones de una historia de esqueleto, con **autenticación real** y
**persistencia real**: cada petición exige un JWT válido y el tenant se deriva del token, no del
cuerpo ni de la query (ADR-018); los tres repositorios secundarios hablan con una SurrealDB real por
HTTP, sin driver Java (ADR-019). El PEP sigue siendo propio (emisor JWT simple, HMAC), no
PDP/OPA/Keycloak todavía — esos siguen siendo evolución prevista detrás de los puertos ya definidos.
La auditoría también sigue siendo dummy en su contenido: registra identificadores, nunca el payload,
como listener de eventos de dominio (ADR-017).

La aplicación **tiene dos secretos reales**: la clave que firma y valida sus JWT y la contraseña de
SurrealDB. No tiene exportador remoto de telemetría todavía. El Key Vault y su cableado, ya en uso
para esos dos secretos, existen para que el resto de credenciales reales no tengan que improvisar
dónde vivir; ver [`infra/README.md`](../infra/README.md).

El POM exige Java 25. Ver la [guía de verificación](evidence/verification-guide.md).
