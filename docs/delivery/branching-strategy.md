# Estrategia de ramas

[← Índice principal](../README.md) · [Pipelines →](pipelines.md)

## Decisión

Se agrega `qa` como rama de integración entre `develop` y `main`:

```text
feature/*  ──►  develop  ──►  qa  ──►  main
                  │            │         │
                  ▼            ▼         ▼
                 DEV          QA       PROD
```

Cada rama de larga vida corresponde exactamente a un ambiente desplegable. Esa correspondencia es
lo que hace que la pregunta "¿qué hay en QA?" tenga una única respuesta verificable: el commit al
que apunta `qa`.

## Justificación

Antes solo existían `main` y `develop`, y el pipeline únicamente disparaba en `main`. Eso obliga a
elegir entre dos cosas malas: validar en producción, o mantener un ambiente de pruebas cuyo
contenido nadie puede reconstruir desde Git.

Se descartó usar tags o ramas de release por versión: para una línea base con un único artefacto
desplegable, añaden ceremonia sin resolver el problema. Se descartó también el despliegue manual a
QA desde `develop`, porque hace que lo probado y lo aprobado sean commits distintos.

## Flujo

| Origen | Destino | Qué ocurre |
|---|---|---|
| `feature/*` | `develop` | PR con build, pruebas y Quality Gate. Sin despliegue. |
| `develop` | — | Despliegue automático a DEV al hacer merge. |
| `develop` | `qa` | PR de promoción. Al hacer merge, despliegue a QA. |
| `qa` | `main` | PR de promoción. Al hacer merge, despliegue a PRODUCCIÓN. |

La promoción siempre es merge de rama a rama y nunca un rebuild desde otro origen: el artefacto que
llega a producción proviene del mismo commit que QA aprobó.

## Protección de ramas en GitHub

`develop`, `qa` y `main` deben tener protección con:

- pull request obligatorio, sin push directo;
- el check del pipeline de Azure DevOps en estado requerido;
- al menos una aprobación en `qa` y `main`;
- historial lineal, para que revertir una promoción sea un solo commit.

Las aprobaciones de despliegue son distintas y viven en Azure DevOps, sobre los objetos
`environment` (`pdp-qa`, `pdp-prod`). Se dejan ahí a propósito: un cambio en el YAML no puede
eliminar una compuerta de producción.

## Creación de las ramas

`qa` no existe todavía en el remoto. Se crea desde `develop` para que su historial sea el de la
línea de integración y no el de producción:

```bash
git checkout main
git pull
git checkout -b develop
git push -u origin develop
git checkout -b qa
git push -u origin qa
```

> `develop` tampoco existe en `origin` al momento de escribir esto — el remoto solo tiene `main`.
> Si ya existiera, basta con `git checkout develop && git pull` antes de crear `qa`.
