# REPORTE — HU-032 (fronteras Modulith del PEP) — 2026-10-30

**Veredicto: APROBADO.**

## Cómo se validó

Flujo completo del harness: `@1-planificador` → compuerta 1 (decisión del responsable sobre `commons`) → `@2-tester-spec`
(`PepModuleDependenciesTests`, 6 casos, rojo por la razón correcta) → `@3-implementador` (solo `package-info.java`) → validación del
responsable: `./mvnw -f pep/pom.xml verify` **con sockets reales** (79 pruebas del PEP, exit 0, incluida `PepArchitectureTests` con sus tres
reglas de capas, que antes no llegaban a ejecutarse porque `verify()` fallaba primero).

## Criterios

| # | Criterio | Resultado | Evidencia |
|---|---|---|---|
| 1 | `PepArchitectureTests` en verde sin relajar reglas ni excluir paquetes | ✅ | La prueba no cambió; antes tenía 23 violaciones de `verify()` |
| 2 | Una dependencia nueva no declarada vuelve a fallar | ✅ | Mutación temporal: una clase en `ingress` que referencia `NormalizeAccessRulesValidator` hizo fallar `verify()` con `Module 'ingress' depends on module 'normalization' via …`; se borró. `PepModuleDependenciesTests` fija además el conjunto declarado (ampliar la lista sin pasar por revisión también falla) |
| 3 | ADR-035 publicada con fronteras y alternativas descartadas | ✅ | `security-platform-architecture/docs/01-governance/adr/ADR-035-pep-module-boundaries.md` |
| 4 | Suite completa de `pep/` en verde | ✅ | `mvnw -f pep/pom.xml verify`: 79 pruebas, exit 0 (en la herramienta sin sockets daba 41 errores de entorno de Netty, no del cambio) |

## Qué cambió

Solo `package-info.java` (sin lógica): `application` OPEN; `enforcement` y `normalization` cerrados con `@NamedInterface("usecase")` y
`@NamedInterface("request")`; `ingress` con las siete dependencias declaradas. `normalization` deja de ser `OPEN`. `commons` queda sin
`package-info` (módulo plano cerrado): un `package-info` allí chocaba con la regla de capas 2, cuyo `..commons..` no puede depender de
`org.springframework..`, y no se toca esa regla.

## Límites conocidos

- La entrada plana `"enforcement"` de `ingress` expone solo la raíz de `enforcement` (que hoy contiene únicamente `ForwardHttpRequest`);
  una prueba fija esa propiedad.
- Si `commons` llegara a tener subpaquetes, habrá que decidir cuáles se publican (ADR-035).
