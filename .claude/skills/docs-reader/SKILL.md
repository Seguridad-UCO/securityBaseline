---
name: docs-reader
description: >-
  Acceso al repositorio de documentación y arquitectura Seguridad-UCO/security-platform-architecture
  (ADRs, event storming, modelo de dominio anémico/enriquecido, historias de usuario, drivers
  arquitectónicos, diagramas C4). Define el mapa de archivos, el protocolo de consulta ordenado
  y el manejo de errores. Usar en la fase de planificación de cualquier HU/HT, antes de preguntar
  al usuario. No usar para consultar el estado del código — para eso está la skill pdp-context.
---

# Skill: docs-reader — Lectura del repositorio de arquitectura

## Qué repositorio es

`Seguridad-UCO/security-platform-architecture` — repositorio **privado** que contiene toda la
documentación de arquitectura y los artefactos de dominio de la Plataforma Central de Seguridad.

**No contiene código ejecutable.** Su carpeta `base_architecture/` es un esqueleto derivado de la
documentación, no un módulo compilable.

---

## Cómo acceder (dos vías, en este orden)

### Vía 1 — Clon local hermano (PREFERIDA)

El repo suele estar clonado como hermano del repo de código:

```
Desktop/Semillero/
├── securityBaseline/                    ← estás aquí
├── securityBaseline-fr/
└── security-platform-architecture/      ← el repo de documentación
```

Verifica primero:

```bash
ls ../security-platform-architecture/docs
```

Si existe, **léelo directamente con Read/Glob/Grep** sobre `../security-platform-architecture/docs/...`.
Es más rápido y no consume cuota de API.

> Antes de confiar en el clon local, comprueba que esté actualizado:
> ```bash
> git -C ../security-platform-architecture log -1 --format="%h %ad %s" --date=short
> ```
> Si el usuario menciona una ADR o artefacto que no encuentras, sugiere `git -C ../security-platform-architecture pull` antes de concluir que no existe.

### Vía 2 — GitHub CLI (si no hay clon local)

```bash
gh auth status
gh api repos/Seguridad-UCO/security-platform-architecture --jq '.full_name'
```

Listar un directorio:
```bash
gh api repos/Seguridad-UCO/security-platform-architecture/contents/docs/01-governance/adr --jq '.[].name'
```

Leer un archivo:
```bash
gh api repos/Seguridad-UCO/security-platform-architecture/contents/docs/02-domain/03-bounded-contexts.md \
  --jq '.content' | base64 -d
```

Buscar por contenido:
```bash
gh api "search/code?q=repo:Seguridad-UCO/security-platform-architecture+{termino}" --jq '.items[].path'
```

---

## Mapa de archivos del repositorio

```
docs/
├── 00-source/              Documento base y línea base fase 0
├── 01-governance/
│   ├── adr/                ADR-001 … ADR-022  ← decisiones de arquitectura
│   └── glossary.md
├── 02-domain/              ← EL MÁS IMPORTANTE PARA PLANIFICAR
│   ├── 02-ubiquitous-language.md
│   ├── 03-bounded-contexts.md      Los 11 BC con responsabilidad y no-responsabilidad
│   ├── 04-context-map.md
│   ├── 05-event-storming.md
│   ├── 06-invariants.md
│   ├── 07-business-rules.md
│   ├── 08-use-cases.md
│   ├── 09-domain-events.md
│   ├── 10-entities.md
│   ├── 11-aggregates.md
│   ├── 12-value-objects.md
│   └── diagrams/models/    01-tenants.html … 11-auditoria-seguridad.html
├── 03-architecture/
│   ├── architectural-drivers.md
│   ├── quality-attributes.md
│   ├── technology-stack.md
│   ├── modulith-dependency-map.md   ← fronteras entre módulos
│   ├── c4/                 level1-context, level2-containers, level3-component-*
│   └── deployment/
├── 04-data/                persistence-model, entity-catalog, indexes
├── 05-contracts/           rest/, events/, policies/, sequence-diagrams/, state-machines/
├── 06-security/            authentication, authorization, threat-model, opa/
├── 07-engineering/         coding-standards, testing-strategy, definition-of-done, ci-cd
├── 08-baseline/            baseline-v1.0, acceptance-checklist, pending-decisions
└── 09-artefactos/          ← ARTEFACTOS DE DOMINIO (event storming, modelos, HU, drivers)
    ├── estrategicos/
    │   ├── vision/
    │   ├── mapa-impacto/
    │   ├── event-storming/         un archivo por bounded context
    │   ├── modelo-dominio/
    │   │   ├── anemico/            estructura: qué objetos y qué atributos
    │   │   └── enriquecido/        detalle: tipo, longitud, obligatorio, único…
    │   └── propuestas-hu/          historias_usuario_priorizadas.md
    └── tecnicos/
        └── drivers-arquitectonicos/
            ├── atributos-calidad/      QA-*.md + tacticas/TAC-*.md
            ├── funcionalidades-criticas/
            ├── restricciones-negocio/
            └── restricciones-tecnicas/
```

---

## Protocolo de Consulta (orden obligatorio al planificar una HU)

### Paso 1 — Localizar la HU

`docs/09-artefactos/estrategicos/propuestas-hu/historias_usuario_priorizadas.md`

Extrae: **Actor**, **Objeto de Dominio**, **Comando**, descripción, prioridad, estimación.

Si la HU no está en ese archivo, pide al usuario el texto completo de la historia.

### Paso 2 — Identificar el bounded context

`docs/02-domain/03-bounded-contexts.md` — tabla de los 11 BC con su responsabilidad y,
crucialmente, **de qué NO es responsable** cada uno. Esa columna evita que metas lógica en
el módulo equivocado.

Luego cruza con la tabla "Bounded Contexts" de la skill `pdp-context` para saber si ese BC
ya tiene módulo Modulith implementado o está pendiente.

### Paso 3 — Event Storming del contexto

`docs/09-artefactos/estrategicos/event-storming/{NN}-{contexto}.md`

Busca el comando que coincide con la HU. Contiene:
- Descripción detallada y actores autorizados
- Información externa / read models
- **Políticas** numeradas (`{Objeto}-POL-01`, …) ← son las reglas de negocio del plan
- Sistemas externos
- Eventos generados
- **Aspectos por solucionar** ← generan preguntas obligatorias al usuario
- Eventos previos y comandos posteriores (para entender el flujo)

### Paso 4 — Modelo de dominio enriquecido (OBLIGATORIO)

`docs/09-artefactos/estrategicos/modelo-dominio/enriquecido/{NN}-{contexto}.md`

De **cada objeto de dominio que la HU afecte**, extrae por atributo:
tipo de dato, longitud, obligatorio, modificable, autogenerado, calculado, sensible,
identifica-al-registro; y las **combinaciones únicas** (restricciones).

Esta información se traduce directamente a código así:

| Característica del modelo | Traducción en el PDP reactivo |
|---|---|
| Obligatorio | `Objects.requireNonNull(x, RequiredArgumentMessages.X)` en el constructor compacto del record |
| Longitud mín/máx | validación explícita en el constructor compacto → lanza `InvalidValueException` |
| Limpiar espacios | `value = value.trim()` en el constructor compacto |
| No modificable | **no** se genera método `withX(...)` para ese campo |
| Autogenerado (UUID) | se recibe de `IdentifierGenerator.next()` en el use case, nunca dentro de la entidad |
| Sensible | fuera de `toString()`, fuera de logs, fuera del `WebResponse` salvo necesidad explícita |
| Combinación única | validación de unicidad en el use case (consulta previa al repositorio) + índice `UNIQUE` en el `SchemaInitializer` de SurrealDB |

> **Nota:** este proyecto **no** tiene Flyway ni JPA. Las restricciones no se declaran con
> `@Column` ni migraciones SQL versionadas, sino en el `Surreal{Modulo}SchemaInitializer`
> (`DEFINE INDEX ... UNIQUE`) y en el constructor del record.

### Paso 5 — ADRs relevantes

`docs/01-governance/adr/` — consulta las que toquen tu HU. Las más citadas:

| ADR | Tema |
|---|---|
| ADR-009 | Spring Modulith |
| ADR-015 | SurrealDB hosting Azure |
| ADR-016 | Capa de interactors |
| ADR-017 | Eventos de dominio con ApplicationEventPublisher |
| ADR-018 | Seguridad JWT reactiva |
| ADR-019 | Implementación SurrealDB (sin driver, saga con compensación) |
| ADR-020 | Keycloak hosting |
| ADR-021 | Retiro de QA como ambiente |
| ADR-022 | Almacenamiento de auth en frontend |

### Paso 6 — Drivers arquitectónicos (si aplica)

Si la HU toca un atributo de calidad no obvio (rendimiento, auditoría, resistencia a ataques),
consulta `docs/09-artefactos/tecnicos/drivers-arquitectonicos/atributos-calidad/QA-*.md`
y sus tácticas asociadas `tacticas/TAC-*.md`.

---

## Manejo de errores

| Situación | Qué hacer |
|---|---|
| No existe el clon local ni `gh` responde | Detente. Informa al usuario y pide el texto de la HU directamente. |
| `gh auth status` falla | Detente. Indica al usuario que ejecute `gh auth login` con acceso a la organización `Seguridad-UCO`. |
| El archivo de event storming del contexto no existe todavía | **No lo inventes.** Informa que el artefacto está pendiente y pide al usuario las políticas y eventos del comando. |
| El modelo enriquecido del contexto está vacío (solo plantilla) | Informa al usuario y pide la especificación de atributos. Sin ella, el plan no puede definir invariantes. |
| La HU contradice una ADR | Detente y reporta. No resuelvas la contradicción por tu cuenta. |

**Regla dura:** una plantilla vacía **no** es información. Si el artefacto existe pero solo
tiene el esqueleto sin llenar, trátalo como "no disponible" y pregunta al usuario.

---

## Registro de fuentes

Anota cada archivo que consultes. El plan generado debe listarlos en su sección de Metadata
para que la decisión sea trazable:

```markdown
- **Fuentes consultadas:**
    - `docs/02-domain/03-bounded-contexts.md`
    - `docs/09-artefactos/estrategicos/event-storming/07-identidad-autenticacion.md`
    - `docs/09-artefactos/estrategicos/modelo-dominio/enriquecido/07-identidad-autenticacion.md`
    - `docs/01-governance/adr/ADR-016-interactor-layer.md`
```
