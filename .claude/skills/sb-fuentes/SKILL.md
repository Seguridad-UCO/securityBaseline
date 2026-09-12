---
name: sb-fuentes
description: Dónde vive la documentación de arquitectura y los artefactos de dominio de la plataforma (security-platform-architecture y artefactos-referencia), qué contiene cada carpeta y en qué orden consultarla al planificar una historia. Incluye la traducción de atributo del modelo de dominio a código. Cargar en la fase de planificación, antes de preguntar al usuario.
---

# Skill: sb-fuentes

Mapa de las fuentes externas al repositorio de código. **Verificado el 2026-08-31** contra los
clones locales — la versión anterior de este mapa describía una carpeta `docs/09-artefactos` que
no existe.

Para el estado del **código**, no uses esta skill: para eso están `sb-arquitectura`,
`sb-estandares` y `pdp/docs/ai-harness/PROJECT-MAP.md`.

---

## Los dos repositorios hermanos

```
Desktop/Semillero/
├── securityBaseline/                    ← el código (aquí trabajas)
├── securityBaseline-fr/                 ← la consola web
├── security-platform-architecture/      ← ADRs, dominio, contratos, C4
└── artefactos-referencia/               ← event storming, modelos, drivers  (sin git)
```

Ambos son **solo lectura** y se consultan por ruta local con `Read`/`Glob`/`Grep`. No hace falta
`gh`: están clonados al lado.

Antes de concluir que algo no existe, comprueba que el clon esté al día:

```bash
git -C ../security-platform-architecture log -1 --format="%h %ad %s" --date=short
```

---

## `security-platform-architecture/docs`

| Carpeta | Qué contiene | Cuándo la abres |
|---|---|---|
| `00-source/` | Documento base y línea base de la fase 0 | Contexto general, rara vez |
| `01-governance/adr/` | **24 ADRs** (`ADR-001` … `ADR-024`) + glosario | Cuando la historia toca una decisión ya tomada |
| `02-domain/` | **Lo más importante para planificar** — ver abajo | Casi siempre |
| `03-architecture/` | Drivers, atributos de calidad, stack, `modulith-dependency-map.md`, C4, despliegue | Si la historia cruza módulos o toca un atributo de calidad |
| `04-data/` | Modelo de persistencia, catálogo de entidades, índices | Si la historia toca la base de datos |
| `05-contracts/` | `rest/`, `events/`, `policies/`, diagramas de secuencia, máquinas de estado | Si la historia expone o consume un contrato |
| `06-security/` | Autenticación, autorización, modelo de amenazas, `opa/` | Si la historia toca permisos o políticas |
| `07-engineering/` | Estándares de código, estrategia de pruebas, definición de terminado, CI/CD | Al validar |
| `08-baseline/` | `baseline-v1.0`, checklist de aceptación, decisiones pendientes | Al cerrar una etapa |
| `templates/` | Plantillas de ADR, decisión, caso de uso, diagrama | Al escribir un ADR nuevo |

### `02-domain/` — el contenido, archivo por archivo

`01-validation-application` · `02-ubiquitous-language` · `03-bounded-contexts` ·
`04-context-map` · `05-event-storming` · `06-invariants` · `07-business-rules` ·
`08-use-cases` · `09-domain-events` · `10-entities` · `11-aggregates` · `12-value-objects`
· `diagrams/`

De estos, los tres que más cambian un plan:

- **`03-bounded-contexts.md`** — los BC con su responsabilidad y, sobre todo, **de qué NO son
  responsables**. Esa columna evita meter lógica en el módulo equivocado.
- **`07-business-rules.md`** — las reglas que la sección 3 del plan debe recoger.
- **`06-invariants.md`** — lo que un value object o una entidad no puede permitir jamás.

---

## `artefactos-referencia/`

```
estrategicos/
├── event-storming/       un archivo .md por contexto (+ los .xlsx originales)
├── mapa-impacto/         mapa_impacto.md y sus versiones
└── modelo-dominio/
    ├── anemico/          qué objetos y qué atributos + diagramas .drawio
    └── enriquecido/      tipo, longitud, obligatorio, único… por atributo
tecnicos/
└── diseno-arquitectonico/drivers-arquitectonicos/
    ├── atributos-calidad/     QA-*.md + tacticas/TAC-*.md
    ├── funcionalidades-criticas/
    ├── restricciones-negocio/
    └── restricciones-tecnicas/
```

> **No hay archivo de historias de usuario priorizadas.** Ni aquí ni en el repo de arquitectura.
> Las historias las dicta el usuario o las deja en `pdp/docs/ai-harness/workspace/HU-XXX.md`.
> Si no encuentras la historia, **pídesela**; no la inventes.

---

## Protocolo de consulta al planificar

1. **La historia.** ¿La dictó el usuario? ¿Está en `pdp/docs/ai-harness/workspace/`? Si no, pídela.
2. **El contexto.** `02-domain/03-bounded-contexts.md` → a qué BC pertenece y de qué **no** es
   responsable. Cruza con la tabla de módulos de `CLAUDE.md` para saber si ese BC ya tiene módulo.
3. **El event storming** del contexto, en `artefactos-referencia/estrategicos/event-storming/`.
   Busca el comando que coincide con la historia: actores, políticas numeradas (esas son las
   reglas de negocio del plan), eventos generados y **«aspectos por solucionar»** — estos últimos
   se convierten en preguntas obligatorias al usuario.
4. **El modelo enriquecido**, en `.../modelo-dominio/enriquecido/`. De cada objeto que la historia
   toque, extrae los atributos y sus restricciones. Se traduce a código con la tabla de abajo.
5. **Los ADRs** relevantes, si la historia toca una decisión ya tomada.
6. **Los drivers**, si toca un atributo de calidad (rendimiento, seguridad, disponibilidad).

Registra en la metadata del plan qué archivos consultaste.

---

## Del modelo de dominio al código

| El modelo dice | En este proyecto se escribe |
|---|---|
| Obligatorio | `Objects.requireNonNull(x, RequiredArgumentMessages.X)` en el constructor compacto |
| Longitud mín/máx | Validación explícita en el constructor compacto del VO → `InvalidValueException` |
| Limpiar espacios | `value = value.trim()` en el constructor compacto |
| No modificable | **No** se genera `withX(...)` para ese campo |
| Autogenerado (UUID) | Viene de `IdentifierGenerator.next()` en el use case, **nunca** dentro de la entidad |
| Marca de tiempo | Viene de `TimeProvider.now()`, nunca `Instant.now()` en línea |
| Sensible | Fuera de `toString()`, fuera de logs y fuera del `WebResponse` salvo necesidad explícita |
| Combinación única | Comprobación en una `Rule` con su puerto **más** `DEFINE INDEX … UNIQUE` en el `Surreal{X}SchemaInitializer` |
| Relación con otro objeto | El identificador del otro agregado como value object, nunca el objeto entero |

> Este proyecto **no** tiene Flyway ni JPA. Las restricciones de esquema no viven en anotaciones ni
> en migraciones SQL versionadas, sino en el `Surreal{X}SchemaInitializer` y en el constructor del
> record.

---

## Reglas invariantes

1. Las fuentes externas son **solo lectura**. Nunca escribes en ellas.
2. Para el estado del código manda el código, no estos documentos.
3. Si no encuentras la historia, la pides. No la inventas.
4. Los «aspectos por solucionar» del event storming son preguntas al usuario, no decisiones tuyas.
