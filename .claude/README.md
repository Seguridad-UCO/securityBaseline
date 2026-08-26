# `.claude/` — Agentes de IA del PDP

Configuración de agentes y skills para desarrollar Historias de Usuario (HU) e Historias
Técnicas (HT) del proyecto `securityBaseline` con Claude Code.

> Este README es documentación para humanos. **Los agentes no deben leerlo** — su contexto
> autoritativo es la skill `pdp-context`.

---

## Estructura

```
.claude/
├── agents/
│   ├── 00-orquestador.md      Coordina el flujo completo. Punto de entrada.
│   ├── 01-planificador.md     HU/HT → PLAN-{TIPO}-{ID}.md
│   ├── 02-implementador.md    PLAN → código, capa por capa
│   ├── 03-tester.md           código → tests JUnit 5 + StepVerifier
│   ├── 04-validador.md        4 niveles de validación + reporte
│   └── 05-commit.md           rama + git add + git commit
└── skills/
    ├── pdp-context/           ← FUENTE DE VERDAD del estado del proyecto
    ├── docs-reader/           acceso al repo de arquitectura
    └── reactive-stack/        Reactor, WebFlux, Modulith, StepVerifier, Java 25
```

La salida de trabajo va a `.workspace/` (no versionada):

```
.workspace/
├── h-plan/PLAN-{TIPO}-{ID}.md            plan generado
└── validator/validator-{TIPO}-{ID}.md    reporte de validación + hash del commit
```

---

## Uso

### La forma fácil — el orquestador

```
@orquestador vamos con la HU-042
```

Detecta en qué etapa está la historia, te dice qué sigue, y va invocando al especialista
correcto en cada paso. Es el punto de entrada recomendado.

```
@orquestador ¿en qué va la HU-042?
```

Solo hace triage y reporta, sin invocar nada.

### Invocación directa

```
@planificador   planifica la HU-042
@implementador  implementa el PLAN-HU-042
@tester         genera los tests para HU-042
@validador      valida HU-042
@commit         ejecuta el commit de HU-042
```

---

## El flujo

```
  @orquestador  ──►  triage: ¿en qué etapa está?
       │
       ├─► @planificador ─► .workspace/h-plan/PLAN-HU-042.md
       │        (consulta event storming, modelo enriquecido y ADRs;
       │         hace preguntas obligatorias; no escribe código)
       │                            ↓ usuario aprueba el plan
       ├─► @implementador ─► código, capa por capa
       │        (domain → application → infrastructure; compila al cerrar
       │         cada capa; auto-corrige hasta 3 intentos)
       │                            ↓
       ├─► @tester ─► tests por capa
       │        (StepVerifier, fakes deterministas; no toca producción)
       │                            ↓
       ├─► @validador ─► .workspace/validator/validator-HU-042.md
       │        (completitud + 26 checks de convenciones + compilación + tests)
       │                            ↓ reporte APROBADO
       └─► @commit ─► rama + commit
                (confirmación explícita; nunca push)
```

**Puntos donde el usuario decide siempre:**
1. Aprobar el plan antes de implementar
2. Aprobar cada capa durante la implementación
3. Confirmar el commit

---

## Qué hace especial a estos agentes

Están adaptados al stack real del proyecto, que difiere bastante de un Spring Boot típico:

| El proyecto usa | Los agentes lo saben |
|---|---|
| **WebFlux reactivo** | Prohíben `block()`, exigen `StepVerifier`, vigilan `switchIfEmpty(Mono.defer(...))` |
| **Records inmutables** como entidades | No generan setters ni `build()`/`rebuild()`; usan factories de negocio y withers |
| **`AggregateRoot` como wrapper** | Nunca `extends AggregateRoot`; emparejan con `AggregateRoot.of(entidad, evento)` |
| **Sin Lombok, sin `@Component`** en use cases | Todo se cablea con `@Bean` en `{Modulo}Configuration` |
| **SurrealDB sin ORM** | SurrealQL con parámetros bind; sin JPA, sin Flyway |
| **Spring Modulith** | Detectan y escalan cualquier cambio a `allowedDependencies` |
| **Eventos vía Spring** | Sin RabbitMQ; `DomainEventPublisher` con `Mono<Void>` |
| **Jackson 3** | `tools.jackson.databind`, nunca `com.fasterxml` |
| **Java 25 + Maven** | `./mvnw`, nunca Gradle |
| **Autorización binaria** | Si una HU pide roles granulares, lo escalan como decisión de arquitectura |

---

## Requisito previo: acceso al repo de arquitectura

El planificador necesita leer `Seguridad-UCO/security-platform-architecture` (event storming,
modelo de dominio enriquecido, ADRs).

**Vía preferida** — tenerlo clonado como hermano:

```
Desktop/Semillero/
├── securityBaseline/                  ← aquí
├── securityBaseline-fr/
├── securityBaseline-infra/
└── security-platform-architecture/    ← el repo de documentación
```

**Vía alternativa** — GitHub CLI:

```bash
gh auth status
gh api repos/Seguridad-UCO/security-platform-architecture --jq '.full_name'
```

Si falla, `gh auth login` con acceso a la organización `Seguridad-UCO`.

---

## Mantener la skill `pdp-context` al día

`pdp-context/SKILL.md` es la única fuente de verdad del estado del proyecto para los agentes.
**Cuando cambie algo estructural, actualízala** — si no, los agentes generarán código
desactualizado con total confianza.

Actualízala cuando:
- se agregue un módulo Modulith nuevo
- cambien las `allowedDependencies` de un módulo
- se suba una versión mayor del stack (Spring Boot, Java)
- se adopte un patrón nuevo (ej. autorización granular cuando lleguen los BC-04/05/08/09/10)
- una ADR nueva contradiga lo que dice la skill

---

## Solución de problemas

**El agente propone código con JPA, Flyway, Lombok o RabbitMQ**
No cargó `pdp-context`, o la skill quedó desactualizada. Recuérdale cargarla explícitamente.

**El agente no encuentra el plan**
Verifica que exista `.workspace/h-plan/PLAN-{TIPO}-{ID}.md` y que el ID coincida exactamente.

**El planificador dice que un artefacto está vacío**
Es correcto: las plantillas de `docs/09-artefactos/` del repo de arquitectura están creadas pero
sin llenar. Hay que completar el event storming y el modelo enriquecido del contexto antes de
planificar en serio esa área.

**`ModulithStructureTests` falla tras implementar**
La HU cruzó una frontera de módulo. **No relajes `allowedDependencies`** para que pase — eso es
una decisión de arquitectura que necesita ADR.

**La compilación falla y el agente insiste**
Tras 3 intentos de auto-corrección escala al usuario con el error exacto y lo que intentó.
Lee ese mensaje: normalmente el plan tenía una firma incorrecta.
