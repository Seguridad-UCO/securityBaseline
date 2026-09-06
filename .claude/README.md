# Harness de IA — cómo se usa

Flujo de desarrollo asistido por agentes. El diseño y su justificación están en
[`docs/ai-harness/README.md`](../docs/ai-harness/README.md).

## Estado

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Skills, herramientas (`mapa`, `verificar`, `drift`, `consistencia`), agentes 1-4, plantillas | ✅ Listo |
| 1b | Deriva doc↔código corregida y verificable · criterios realineados a 18/23 | ✅ Listo |
| 2 | `2-tester-spec` y `3-implementador` ✅ · slash commands, mutation testing y `5-entrega` ⏳ | 🟡 Parcial |
| 3 | Grafo nivel 1 y 2 | ⏳ Pendiente |

**Próximo paso:** ejecutar **HU-001** (búsqueda con criterios y paginación + prueba HTTP end-to-end)
por el flujo agéntico. Cierra los criterios 10 y 16-19, y es la primera prueba real del harness.

---

## El ciclo, hoy

```
  Historia
     │
     ▼
  @1-planificador  →  PLAN-{ID}.md  +  esqueletos de la SPEC (compilan, sin lógica)
     │
     ▼
  [ GATE 1 — humano: ¿el contrato es correcto? ]
     │
     ▼
  (fase 2: @2-tester-spec en rojo → @3-implementador en verde)
     │           mientras tanto, este tramo lo haces tú
     ▼
  @4-validador  →  REPORTE-{ID}.md
     │
     ▼
  [ GATE 2 — humano: antes de que salga del repositorio ]
```

## Invocar los agentes

```
@1-planificador planifica HU-012: registrar un recurso protegido
@4-validador valida HU-012
```

---

## Herramientas

Ambas asumen que las ejecutas desde la raíz del repositorio.

**Mapa del proyecto** — inventario por slice y por rol, puertos, endpoints y pruebas.
Es el nivel 0 del grafo de conocimiento: se consulta antes de leer código.

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/mapa.ps1
```

`-Check` sale con código 1 si el mapa quedó desactualizado respecto al código (útil en CI).

**Verificación** — corre Maven y devuelve solo el resumen, no las cientos de líneas del log.
Selecciona por su cuenta el JDK que exige el POM, aunque `JAVA_HOME` apunte a otro.

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/verificar.ps1
```

| Flag | Efecto |
|---|---|
| *(ninguno)* | `mvnw verify` — compila, prueba, cobertura, arquitectura |
| `-Rapido` | `mvnw test` — sin cobertura |
| `-Compilar` | `mvnw test-compile` — solo compila |
| `-Prueba X` | Una clase de prueba concreta |
| `-Lineas N` | Más contexto por fallo (por defecto 12) |

El log completo queda en `target/verificar-ultimo.log` (ignorado por git).

**Consistencia arquitectónica** — comprueba que todos los slices tengan la misma forma. ArchUnit
verifica la *dirección* de las dependencias y Modulith el mapa entre módulos; ninguno comprueba que
un slice se parezca al de al lado, y ahí es donde uno se pierde.

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/consistencia.ps1
```

`-Estricto` sale con código 1 si hay hallazgos. Una divergencia legítima —una capacidad que ese
slice no necesita— se declara en `docs/ai-harness/consistencia-ignore.txt` con su razón.

**Deriva doc↔código** — comprueba que todo enlace de `docs/` y de `.claude/` resuelva y que toda
clase citada exista.
Es la prueba de regresión del fallo histórico del proyecto: documentación que afirma lo que el código
no sostiene.

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/drift.ps1
```

`-Estricto` sale con código 1 si hay hallazgos. Las excepciones se declaran en
`docs/ai-harness/drift-ignore.txt`, **solo** para lo que está pendiente a propósito y marcado como
tal en el documento que lo menciona.

---

## Skills

Se cargan solas cuando un agente las invoca. También puedes pedirlas por nombre.

| Skill | Cubre |
|---|---|
| `sb-arquitectura` | Slices, capas, contratos base, dónde vive cada archivo |
| `sb-estandares` | Idioma, value objects, excepciones, mensajes, DTOs, cableado |
| `sb-reactivo` | Trampas de Boot 4 / Jackson 3 / Java 25, operadores Reactor, Modulith, StepVerifier |
| `sb-testing` | JUnit + AssertJ + StepVerifier, fakes en vez de Mockito, antipatrones |
| `sb-criterios` | Los 23 criterios como contrato de aceptación, y cuáles no se cumplen |
| `sb-fuentes` | Qué hay en los repos hermanos y en qué orden consultarlo al planificar |

**Las skills se derivan del código, no de `docs/`.** Cuando cambies una convención en el código,
actualiza la skill en el mismo cambio — si no, los agentes multiplicarán la versión vieja.

---

## Artefactos generados

| Ruta | Qué es | Se versiona |
|---|---|---|
| `docs/ai-harness/PROJECT-MAP.md` | Mapa generado. **No editar a mano** | Sí |
| `docs/ai-harness/workspace/planes/` | Contratos por historia | Sí |
| `docs/ai-harness/workspace/reportes/` | Reportes de validación | Sí |
| `target/verificar-ultimo.log` | Log completo del último build | No |
