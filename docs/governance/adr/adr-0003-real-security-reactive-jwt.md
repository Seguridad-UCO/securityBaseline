# ADR-0003: Seguridad real con Spring Security reactivo y JWT

[← Gobierno](../README.md)

## Estado

**Implementada.** Ver [Nota de implementación](#nota-de-implementación) para las decisiones concretas
que no estaban fijadas cuando se aceptó esta ADR.

## Contexto

Hoy no hay autenticación ni autorización en tiempo de ejecución: `spring-boot-starter-security` no
está en el POM. El `TenantId` llega en el cuerpo o en la query sin verificar que quien lo envía
puede operar sobre ese tenant. Es coherente con una línea base de adaptadores dummy, pero deja
abierta la brecha entre el propósito del PDP y su comportamiento operativo.

## Decisión

Se incorporará Spring Security **reactivo** (sobre WebFlux) con validación JWT como frontera PEP.
El `TenantId` y el sujeto autenticado se derivarán de los *claims* del token —no del cuerpo ni de
la query— y se propagarán vía `RequestContext`.

## Justificación

1. **Cierra la brecha operativa.** El PDP (tenants, aplicaciones, recursos, acciones) necesita un
   PEP real delante para tener sentido en ejecución.
2. **Reactivo, no MVC.** El proyecto es WebFlux; la cadena de seguridad debe ser
   `SecurityWebFilterChain` / `ReactiveAuthenticationManager`.
3. **Tenant desde el principal.** El tenant sale de un *claim* firmado del JWT, no de una cabecera
   o campo editable por el cliente.
4. **Mismos contratos de error.** Los 401/403 reutilizarán `ApiResponse` / RFC7807 de
   `ApiErrorHandler`, sin un formato paralelo.
5. **Sin reintroducir Jakarta Validation.** La validación JWT (firma, expiración, emisor) es
   independiente de la estrategia de entrada documentada en
   [13. Estrategia de entrada](../interfaces/13-input-strategy-dtos.md).

## Alternativas consideradas

- **Documentar solo el puerto y posponer autenticación.** Descartada: el alcance incluye seguridad
  real.
- **Spring Security MVC (Servlet).** Incompatible con WebFlux.
- **Keycloak/OPA desde el día uno.** Visión a largo plazo; primero se valida el flujo JWT reactivo
  end-to-end con un emisor simple.

## Consecuencias

- Dependencias: `spring-boot-starter-security` + `spring-boot-starter-oauth2-resource-server`
  (reactivos).
- Paquete `shared/security`: `SecurityConfiguration` (cadena de filtros + `ReactiveJwtDecoder`),
  `PdpPrincipal`, `SecurityContext`, `ApiAuthenticationEntryPoint`, `ApiAccessDeniedHandler`.
- Los interactores de `recursos` (ADR-0001) leen el principal autenticado antes de mapear.
- Rutas públicas acotadas (`/actuator/health`, `/actuator/info`); el resto exige token válido.
- Pruebas de integración WebFlux (`WebTestClient`): `SecurityWebFilterChainTests` (401/403/rutas
  públicas) y `ProtectedApplicationHttpTests` (flujo de negocio ya autenticado, incluye aislamiento
  entre tenants).

## Nota de implementación

Decisiones que no estaban fijadas en el texto original de esta ADR y se tomaron durante la etapa 3:

- **`spring-boot-starter-oauth2-resource-server`, no una librería JWT suelta.** Trae
  `NimbusReactiveJwtDecoder` ya integrado con `SecurityWebFilterChain`. El emisor propio de hoy usa
  `NimbusReactiveJwtDecoder.withSecretKey(...)` (HMAC); el día que Keycloak lo reemplace, el cambio
  es `.withJwkSetUri(...)` sin tocar el resto de la cadena.
- **`tenantId` se retiró por completo del cuerpo y la query**, no solo se ignora si llega. Un campo
  presente pero ignorado habría sido código muerto y una fuente de confusión ("¿por qué mando esto
  si no hace nada?"). Como consecuencia, `ProtectedApplicationCriteria.tenantId` pasó de
  `Optional<TenantId>` a `TenantId` obligatorio: ya no existe una consulta sin tenant, y
  `SearchProtectedApplicationsRulesValidatorImpl` aplica `TenantMustBeActiveRule` siempre, no solo
  cuando el filtro estaba presente.
- **El secreto de firma vive en `application.properties` como valor de desarrollo**, marcado
  explícitamente como tal (`dev-only-signing-key-not-for-production-use`), porque el emisor propio
  necesita alguno para funcionar localmente y en pruebas. `application-qa.properties` y
  `application-prod.properties` lo redefinen sin valor de respaldo (`${PDP_JWT_SIGNING_KEY}`), así
  que un despliegue real sin la variable de entorno falla al arrancar en vez de operar en silencio
  con la clave de desarrollo. Ver [`infra/README.md`](../../infra/README.md).
- **Jackson 3, no Jackson 2.** Spring Boot 4 / Spring Framework 7 renombraron el paquete base de
  `com.fasterxml.jackson` a `tools.jackson`. Los handlers 401/403
  (`ApiAuthenticationEntryPoint`/`ApiAccessDeniedHandler`) serializan `ProblemDetail` con el
  `ObjectMapper` de Spring, y esto solo compila importando `tools.jackson.databind.ObjectMapper`.
