# 23. Capacidades transversales antes del negocio

[← Excepciones](09-exception-handling.md) · [↑ Transversales](README.md)

## Decisión arquitectónica

La línea base entrega dos operaciones útiles —registrar y consultar— pero prioriza contratos,
validación, reglas explícitas, transacciones, observabilidad, errores, dummies, pruebas, pipeline y
manejo de secretos antes de agregar historias de autorización.

## Justificación

El riesgo principal señalado por el asesor es construir funcionalidades sin soporte transversal. Se
descarta ampliar a PEP/PDP/OPA antes de probar que una historia mínima respeta los límites.

## Implementación

E-1 ejerce todos los mecanismos que reutilizarán E-2 a E-6:

- puertos de entrada y salida con convención de firmas explícita;
- reglas separadas por si usan repositorio, con excepción propia cada una;
- estrategia de entrada en dos niveles, sin Jakarta Validation;
- transacción con rollback demostrable y compensación entre módulos;
- correlación y logging estructurado;
- criterio, paginación y rangos;
- estructura Modulith verificada por prueba;
- pipeline con Quality Gate en tres ambientes;
- Key Vault provisionado antes de que exista el primer secreto.

Ese último punto es el ejemplo más claro del criterio: el mecanismo de secretos está listo **antes**
de tener secretos, de modo que la primera credencial real no tenga que improvisar dónde vivir.

## Ubicación verificable

- [Índice de criterios](../baseline-criteria-overview.md)
- [Pruebas](../../src/test/java/co/edu/uco/seguridad)
- [Pipelines](../delivery/pipelines.md) y [Key Vault](../../infra/README.md)
- [Diseño de implementación](../plans/2026-08-03-security-baseline-design.md)

## Evidencia y límite

**109 pruebas** verifican el esqueleto, con 92,7 % de cobertura de instrucciones. No se declara
completa la seguridad de runtime: PEP, PDP, OPA, Keycloak y SurrealDB son el siguiente incremento y
deberán reutilizar estas capacidades en lugar de crear las suyas.
