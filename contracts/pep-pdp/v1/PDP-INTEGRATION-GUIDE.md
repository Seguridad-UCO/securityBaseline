# Guía breve para completar PEP ↔ PDP v1

## Estado comprobado

El PEP ya consume `POST /internal/v1/access-decisions` mediante `WebClientDecisionAdapter`. Envía el Bearer
original como evidencia de identidad, `X-Request-Id`, `X-Correlation-Id` y el cuerpo definido en
`request.schema.json`. Para HTTPS configura CA y certificado/clave cliente; fuera de desarrollo exige mTLS.
No hay una implementación de esa operación en el PDP actual, por lo que no se modificó el PDP.

El PEP falla cerrado: `ALLOW` completo y sin obligaciones es la única respuesta que reenvía al backend.
`DENY` produce 403 (o 401 para `TOKEN_INVALID`), mientras que errores de transporte, contrato, PDP u OPA
producen 503 y no se reenvía la solicitud.

## Trabajo requerido en el PDP

1. Exponer `POST /internal/v1/access-decisions` exactamente como
   [openapi.yaml](openapi.yaml), en la cadena interna y separada del BFF.
2. Autenticar el servicio PEP con mTLS y rechazar certificados no admitidos. Validar también el JWT recibido
   como evidencia, incluyendo issuer, firma, caducidad y audiencia para `application.id`; no confiar en roles,
   tenant ni atributos enviados por el PEP.
3. Resolver sujeto, tenant, recurso catalogado y la acción a partir de los atributos confiables. En v1,
   `resource.path` es la ruta relativa que recibe el backend y `resource.action` coincide con el verbo HTTP.
4. Evaluar la política en OPA y persistir/publicar la evidencia de auditoría en el PDP. No devolver un
   `ALLOW` provisional mientras OPA, catálogo o identidad estén incompletos.
5. Responder HTTP 200 + JSON para una evaluación completa, con los mismos `requestId` y `correlationId` del
   request y sus headers. Usar exclusivamente `ALLOW`, `DENY` o `INDETERMINATE`; `obligations` debe estar
   ausente, ser `null` o una lista vacía en v1.
6. Exponer `GET /actuator/health` para la readiness del PEP. Esa sonda no toma decisiones.

## Configuración que ya espera el PEP

```properties
pep.pdp.base-url=https://pdp.internal
pep.pdp.ca-certificate=/run/secrets/pdp-ca.pem
pep.pdp.client-certificate=/run/secrets/pep-cert.pem
pep.pdp.client-key=/run/secrets/pep-key.pem
pep.pdp.connect-timeout=1s
pep.pdp.timeout=3s
pep.pdp.max-response-bytes=65536
```

En desarrollo aislado se puede usar HTTP únicamente con `pep.pdp.allow-insecure-http=true`. No trasladar esa
configuración a producción.

## Validación final

1. Arrancar el PDP con mTLS y health disponible; confirmar que readiness del PEP es `UP`.
2. Registrar una ruta PEP y un recurso/política PDP que permita el JWT de prueba: la URL pública debe devolver
   200 y el backend debe recibir la ruta sin `/apps/{application-id}`.
3. Cambiar la política a deny: debe devolver 403 y el backend no debe recibir la solicitud.
4. Detener OPA o el endpoint de decisión: debe devolver 503 y el backend no debe recibir la solicitud.
5. Ejecutar `./mvnw -f pep/pom.xml verify` para verificar que el cliente PEP y el contrato v1 siguen
   alineados.
