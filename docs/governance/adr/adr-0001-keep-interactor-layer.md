# ADR-0001: Conservar la capa de interactor

[← Gobierno](../README.md)

## Estado

Aceptada e implementada.

## Contexto

Cada operación expuesta del PDP pasa por un interactor de un solo método (que extiende
`ReactiveOperation` / `ReactiveOperationWithoutResult`) antes de llegar al caso de uso. El
controlador (o el módulo consumidor) no conoce el caso de uso: solo ejecuta el interactor.

El interactor es el lugar del mapeo entre el payload de entrada y el DTO tipado de aplicación, y
entre el resultado de dominio y la proyección de salida. El caso de uso orquesta reglas y
persistencia sobre tipos de aplicación/dominio, sin conocer HTTP.

## Decisión

Se conserva la capa de interactor como puerto primario explícito, con una interfaz por operación
(un solo `execute`), en vez de fusionarla con el caso de uso o agrupar varias operaciones en una
fachada multi-método.

## Justificación

1. **Puerto primario claro.** El adaptador (HTTP u otro módulo) depende del interactor, no del
   caso de uso ni de mappers web.
2. **Punto de extensión para seguridad.** Cuando entre autenticación real (ADR-0003), el
   interactor es el lugar natural para leer el `RequestContext` autenticado (tenant y sujeto)
   sin tocar el caso de uso.
3. **Una operación, un contrato.** Evita fachadas que encapsulan varios casos de uso detrás de
   una misma interfaz con múltiples métodos.

## Alternativas consideradas

- **Eliminar el interactor y que el controlador llame directo al caso de uso.** Reduce una clase
  por operación, pero mezcla responsabilidades de transporte con orquestación y elimina el punto
  donde ADR-0003 enganchará el contexto de seguridad.
- **Fachada multi-método por módulo.** Descartada: mezcla operaciones distintas en un solo
  contrato y oculta el patrón genérico → interfaz individual.

## Consecuencias

- El controlador solo recibe el payload, ejecuta el interactor y envuelve la respuesta HTTP.
- Los módulos se publican entre sí vía interactors (`RegisterApplicationInteractor`,
  `RemoveApplicationInteractor`, `FindTenantInteractor`, etc.), no vía APIs multi-método.
- El mapeo raw → tipado → dominio → respuesta vive en el interactor; el caso de uso trabaja con
  dominio/aplicación.
