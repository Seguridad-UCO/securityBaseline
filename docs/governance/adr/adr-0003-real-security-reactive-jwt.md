# ADR-0003: Seguridad real con Spring Security reactivo y JWT

[← Gobierno](../README.md)

## Estado

Aceptada — pendiente de implementación.

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

- Dependencias: `spring-boot-starter-security` (reactivo) + librería JWT.
- Paquete `shared/security`: cadena de filtros, auth JWT, handlers 401/403.
- El interactor (ADR-0001) leerá el `RequestContext` autenticado.
- Rutas públicas acotadas (`/actuator/health`, `/actuator/info`); el resto exige token válido.
- Pruebas de integración WebFlux (`WebTestClient`) para 401/403 y propagación de tenant.
