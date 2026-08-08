# Evidencia arquitectónica de la línea base

Esta documentación sustenta la HU E-1 / UC-02: registrar una aplicación protegida con tenant,
nombre y un recurso con su acción, y consultar el catálogo resultante. Cada criterio solicitado por
el asesor tiene una página que responde qué se decidió, por qué, cómo se implementa y dónde se
verifica.

## Navegación por tema

- [Enfoque general de los 23 criterios](baseline-criteria-overview.md): índice para sustentación con
  el estado real de cada criterio.
- [Matriz de cumplimiento](criteria-compliance-matrix.md): estado inicial, problema encontrado,
  cambio realizado y estado final, criterio por criterio.
- [Alineación PDP / Spring Modulith](architecture/pdp-modulith-alignment.md): estructura y
  dependencias ejecutables frente a la arquitectura de referencia.
- [Arquitectura](architecture/README.md): 1, 2, 11, 12, 20, 21 y 22.
- [Dominio y datos](domain-and-data/README.md): 3, 10 y 15 a 19.
- [Interfaces](interfaces/README.md): 5, 6, 13 y 14.
- [Capacidades transversales](cross-cutting/README.md): 4, 8, 9 y 23.
- [Infraestructura](infrastructure/README.md): 7.
- [Entrega](delivery/README.md): ramas, pipelines, SonarQube y secretos.
- [Evidencia y navegación de código](evidence/README.md).

## Alcance honesto

La línea base implementa dos operaciones de una historia de esqueleto con adaptadores dummy. No
declara como realizados PEP, PDP, OPA, Keycloak ni SurrealDB: son evolución prevista detrás de los
puertos ya definidos.

La aplicación **no tiene ningún secreto todavía**, porque no tiene base de datos, proveedor de
identidad ni exportador remoto de telemetría. El Key Vault y su cableado existen desde ahora para
que la primera credencial real no tenga que improvisar dónde vivir; ver
[`infra/README.md`](../infra/README.md).

El POM exige Java 25. Ver la [guía de verificación](evidence/verification-guide.md).
