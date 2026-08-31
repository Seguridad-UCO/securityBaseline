<!--
Plantilla del reporte de validacion. La produce y la escribe @4-validador (un agente, un artefacto).

Destino: docs/ai-harness/workspace/reportes/REPORTE-{HU|HT}-{ID}.md

Ninguna seccion es opcional. Una seccion sin hallazgos se deja en "Ninguno" — borrarla la hace
indistinguible de un olvido.
-->

# Reporte de validacion — {HU|HT}-{ID}

## Metadata

- **Slice:** `{slice}`
- **Fecha:** {yyyy-MM-dd}
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-{HU|HT}-{ID}.md`
- **Rama:** `feature/{HU|HT}-{ID}-{descripcion}`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`. Pegar el resumen tal cual, no el log.

```
{salida de verificar.ps1}
```

| Comprobacion | Resultado |
|---|---|
| Compilacion | {✅ / ⛔} |
| Pruebas | {✅ N pruebas / ⛔ N fallos} |
| Cobertura (≥ 50 % por paquete) | {✅ / ⛔ / N/A} |
| `LayeredArchitectureTests` | {✅ / ⛔} |
| `ModulithStructureTests` | {✅ / ⛔} |

## Estado final

> ✅ APROBADO — sin bloqueantes. / ⛔ RECHAZADO — hay {N} bloqueante(s).

**Un solo bloqueante = RECHAZADO**, aunque todo lo demas este bien.

## Bloqueantes

### [{juicio o criterio}] — {titulo}

- **Archivo:** `{ruta relativa desde la raiz del repo}`
- **Problema:** {que esta mal}
- **Referencia:** {regla de sb-arquitectura / sb-estandares / criterio de la linea base}
- **Correccion esperada:** {que debe hacer el implementador}

{o "Ninguno"}

## Observaciones menores

{Mismo formato, o "Ninguno". No bloquean la entrega, pero se registran.}

## Los cuatro juicios

> Lo que ninguna prueba puede verificar. Cada uno se responde con evidencia, no con una impresion.

| # | Juicio | Resultado | Evidencia |
|---|---|---|---|
| 1 | ¿Cumple los criterios de aceptacion del plan (no solo compila)? | {✅/⛔} | {criterio → prueba o archivo} |
| 2 | ¿Convencion de idioma? (codigo en ingles, mensajes en espanol) | {✅/⛔} | |
| 3 | ¿Introdujo deriva doc↔codigo? | {✅/⛔} | |
| 4 | ¿La logica quedo en la capa correcta? | {✅/⛔} | |

## Criterios de la linea base

> Solo los que el plan declaro. Los marcados 🤖 los resuelve el build, no la lectura.

| # | Criterio | Resultado | Punto de control comprobado |
|---|---|---|---|

## Desviaciones respecto al plan

| Archivo | Plan decia | Codigo hace | ¿Justificado? |
|---|---|---|---|

{o "Ninguna"}

## Datos para la entrega

> Insumo de la fase de entrega. Un dato que no dejes aqui es algo que ese paso no podra hacer.

- **Mensaje de commit:** `{tipo}({slice}): {descripcion corta}`
- **Cuerpo:** {que se implemento, capas tocadas, endpoints, tabla}
- **Rama:** `feature/{HU|HT}-{ID}-{descripcion}`
- **Archivos a incluir:** {solo codigo y pruebas — el plan y este reporte se versionan aparte, en docs/ai-harness/workspace/}

## Proximos pasos

{Si APROBADO: "Listo para el gate 2 (entrega)."}
{Si RECHAZADO: "@3-implementador corrige los bloqueantes y se repite la validacion."}
