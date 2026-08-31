<!--
Plantilla del plan. La produce @1-planificador y la lee @4-validador.

Destino: docs/ai-harness/workspace/planes/PLAN-{HU|HT}-{ID}.md

Secciones 1-4 y 8-10: siempre. Secciones 5-7: condicionales, borra la que no aplique.
La seccion 7 (SPEC) es el contrato: es lo unico que ve el tester, y compila antes que la logica.
-->

# PLAN: {Titulo}

## Metadata

- **ID:** {HU|HT}-{ID}
- **Slice:** `{applications|identity|resources|tenants}` (o `nuevo: {nombre}`)
- **Tipo:** {Escritura | Consulta | Mixto}
- **Fecha:** {yyyy-MM-dd}
- **Rama sugerida:** `feature/{HU|HT}-{ID}-{descripcion-en-kebab}`
- **Fuentes:** {rutas leidas de security-platform-architecture / artefactos-referencia, o "dictada por el usuario"}
- **Criterios de la linea base que toca:** {lista de numeros — ver skill `sb-criterios`}

## 1. Resumen funcional

{2-4 oraciones: que hace y que NO cubre.}

## 2. Criterios de aceptacion

| # | Criterio | Resultado esperado |
|---|---|---|

## 3. Reglas de negocio

> Invariante **local** (formato, longitud, obligatoriedad del propio valor) → constructor compacto
> del value object → `InvalidValueException` → **400**.
> Restriccion de **conjunto** (unicidad, existencia, estado de otro agregado) → una clase `Rule` con
> su interfaz y su impl → `BusinessRuleViolationException` (**400**) o `ConflictBusinessRuleException`
> (**409**).
> **Nunca `if/throw` de negocio dentro del use case.**
>
> Antes de declarar una `Rule`, pregunta si el caso debe **lanzar**. Si solo decide si vale la pena
> seguir y su resultado no es un error, es una consulta al puerto con `if (...) return;`.
> Si la regla no hace I/O, es **sincrona** (`Operation`/`OperationWithoutResult`), no reactiva.

| # | Regla | Donde vive (VO / Rule) | Puerto que trae el dato | Excepcion → HTTP |
|---|---|---|---|---|

## 4. Modelo de dominio afectado

### Entidad / agregado

{Nombre, si es nuevo o existente, factoria con nombre y comportamiento que expone.}

### Value objects

| VO | Nuevo o existente | Invariantes | Vive en |
|---|---|---|---|

### Enums

| Enum | Valores | Comportamiento que expone |
|---|---|---|

## 5. Persistencia — borrar si la historia no toca la base de datos

- **Tabla:** `{nombre}` (constante en `{X}Schema`)
- **Campos:** {lista}
- **Consultas nuevas en el puerto:** {firmas}
- **Inicializador de esquema:** {existente | nuevo}

## 6. Endpoint — borrar si la historia no expone HTTP

| Verbo | Ruta | Codigo de exito | Cuerpo de entrada | Cuerpo de salida |
|---|---|---|---|---|

- **Autorizacion:** {publica | requiere token; el inquilino sale del principal, nunca del body}
- **Errores esperados:** {codigo de excepcion → HTTP}

## 7. SPEC — el contrato

> Esto es lo que `@2-tester-spec` recibe. **Firmas exactas, sin cuerpos.** Cada interfaz aqui debe
> compilar tal cual. Las implementaciones se generan como esqueletos que lanzan
> `UnsupportedOperationException`, para que el contrato exista antes que la logica.

### Contratos nuevos

```java
// {ruta relativa desde src/main/java}
public interface {Nombre} extends {ContratoBase}<{Entrada}, {Salida}> {
}
```

### Firmas de value objects y entidades

```java
// {ruta}
public record {Nombre}({tipo} {campo}) { }          // invariantes: {resumen}
```

### Firmas de DTOs

```java
// {ruta}
public record {Nombre}({tipos y nombres}) { }
```

### Firmas nuevas en puertos existentes

```java
// {ruta del puerto}
Mono<{T}> {metodo}({Tipo} {arg});
```

## 8. Arbol de archivos

> Rutas completas desde `src/main/java/co/edu/uco/seguridad/`. Marca `[N]` nuevo, `[M]` modificado.
> Toda clase nueva de `application` debe aparecer tambien como `[M]` en `{Slice}Configuration`.

```
pdp/{slice}/
├── domain/
├── application/
└── infrastructure/
```

## 9. Casos de prueba esperados

> El presupuesto y las convenciones estan en la skill `sb-testing`. Nada de Mockito.

| Capa | Clase de prueba | Casos |
|---|---|---|

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | {fecha} |
| Contrato aprobado (gate 1) | ⏳ Pendiente | |
| Pruebas en rojo | ⏳ Pendiente | |
| Implementacion en verde | ⏳ Pendiente | |
| Validacion | ⏳ Pendiente | |
| Entrega (gate 2) | ⏳ Pendiente | |

## 11. Ambiguedades pendientes

{Lo que el planificador no pudo resolver y espera del usuario, o "Ninguna".}
