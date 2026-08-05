# Diseño aprobado: alineación de la línea base con el PDP Modulith

## Decisión

`securityBaseline` representa el contenedor PDP de la arquitectura de referencia. Se crean los módulos Spring Modulith `tenants`, `aplicaciones` y `recursos`, con `commons` como shared kernel no modular. Cada módulo conserva `domain`, `application` e `infrastructure`.

## Dependencias

`aplicaciones` depende de `commons` y de la API pública de `tenants`. `recursos` depende de `commons`, `tenants` y de la API pública de `aplicaciones`. No se permite la dirección inversa. El módulo `recursos` orquesta E-1: valida tenant, registra aplicación y registra recurso inicial en la misma frontera transaccional dummy.

## Justificación

La versión anterior concentraba tres bounded contexts en `applications`; eso contradice el mapa Modulith aceptado y ocultaba los contratos entre módulos. La estructura nueva evidencia APIs públicas, ownership y dirección de dependencias sin crear los contextos que no intervienen en E-1.

## Límites

Los repositorios y tenants son dummies en memoria; SurrealDB, OPA, IdP y auditoría remota siguen fuera de la HU. Sus futuros adaptadores reemplazarán puertos sin modificar dominio ni APIs públicas de módulo.
