# 23. Capacidades transversales antes del negocio

## Decisión arquitectónica

La línea base entrega una sola HU útil, pero prioriza contratos, validación, transacciones, observabilidad, errores, dummies y pruebas antes de agregar historias de autorización.

## Justificación

El riesgo principal señalado por el asesor es construir funcionalidades sin soporte transversal. Se descarta ampliar a PEP/PDP/OPA antes de probar que una historia mínima respeta los límites.

## Implementación

E-1 usa todos los mecanismos que después reutilizarán las historias E-2 a E-6: puertos, Reactor, correlación, auditoría, transacción y estructura Modulith. La carpeta `docs/` funciona como evidencia auditable de esa prioridad.

## Ubicación verificable

- [Índice de criterios](../README.md).
- [Pruebas](../../src/test/java/co/edu/uco/seguridad).
- [Diseño de implementación](../plans/2026-08-03-security-baseline-design.md).

## Evidencia y límite

Ocho pruebas verifican el esqueleto. No se marca como completa la seguridad de runtime: PEP/PDP/OPA/IdP son el siguiente incremento y deberán reutilizar estas capacidades.
