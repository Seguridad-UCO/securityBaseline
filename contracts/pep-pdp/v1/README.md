# Contrato PEP–PDP v1

Este contrato es una **nueva interfaz acordada**, no un endpoint ya implementado en el PDP.
El cliente real está implementado en el PEP; el simulador vive exclusivamente en sus fuentes de test.

## Semántica

- JSON UTF-8, POST, respuesta estrictamente HTTP 200 y Content-Type application/json para decisiones.
- Headers y cuerpo llevan el mismo requestId/correlationId. El PEP genera un UUID requestId por solicitud;
  acepta correlationId solo con caracteres y longitud acotados. No representan identidad ni autorización.
- El token original va exclusivamente en Authorization. El contrato no contiene tokens, roles o tenants
  aportados por el llamador. Nunca registrar Authorization, cookies, cuerpo de negocio ni query.
- application.id y environment vienen de configuración confiable del PEP; el PDP verifica que el servicio
  PEP autenticado por mTLS puede evaluar esa aplicación/entorno.
- resource.path es la ruta relativa exacta que el backend recibirá, sin prefijo público ni query.
  resource.action y context.method son el mismo verbo HTTP en v1.
- El PDP debe resolver el recurso del catálogo y traducir el verbo a la acción de política cuando corresponda.
  No asumir que el catálogo actual ya tiene implementado matching de plantillas o evaluación OPA.
- Query y cuerpo se reenvían a la aplicación pero no son atributos de autorización en v1. Una operación cuya
  identidad de recurso dependa de query/body necesita una extensión de contrato y contexto confiable antes
  de integrarse; no puede confiar en una decisión calculada para otro recurso.
- La identidad local y tenant se resuelven en el PDP mediante evidencia validada. Roles/perfiles vigentes
  son entradas de OPA; el PEP no usa claims de roles para decidir.

## Respuesta y compatibilidad

Campos obligatorios: decision, decisionId, reasonCode, policyReferences, requestId y correlationId.
Obligations puede omitirse, ser null o una lista vacía. Una obligación no vacía causa 503, incluso con ALLOW.
No se interpreta cacheDirective: nunca hay reutilización de decisiones.

Solo enums conocidos y decisiones completas/correlacionadas son aplicables. DENY/TOKEN_INVALID y HTTP 401
se traducen a 401; demás DENY a 403; INDETERMINATE y errores de protocolo/transporte a 503.
Los códigos y referencias de políticas no se devuelven sin filtrar al cliente externo.
Un ALLOW con TOKEN_INVALID es inconsistente y causa 503.

Cambios aditivos no críticos pueden conservar v1; campos desconocidos de respuesta no confieren
capacidades nuevas. Obligaciones nuevas y cambios semánticos requieren negociación/versionamiento.
Una versión incompatible deberá usar otra ruta. Los ejemplos y esquemas son independientes de Java.

## Conexión futura del PDP

Implementar /internal/v1/access-decisions con cadena de seguridad propia (mTLS + JWT), aislada del BFF.
Validar issuer/audience según la aplicación solicitada, resolver sujeto/tenant y recursos, invocar OPA,
emitir evidencia de auditoría y devolver IDs correlacionados. No habilitar ALLOW provisional en Java.
El PEP conserva su puerto y cliente; cambian URL y certificados de configuración.

El PDP debe ofrecer GET /actuator/health para readiness del cliente. Esta sonda no evalúa acceso.
La verificación TLS debe validar CA, vigencia y hostname; el PEP no contiene trust-all.
HTTP solo se admite cuando allow-insecure-http se configura explícitamente para desarrollo.

