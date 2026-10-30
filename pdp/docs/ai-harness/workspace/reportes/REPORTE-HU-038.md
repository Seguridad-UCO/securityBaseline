# REPORTE — HU-038 (identidad local en el canal Bearer) — 2026-10-30

**Veredicto: APROBADO.**

## Cómo se validó

Flujo del harness: `@1-planificador` → decisiones cerradas en [HU-038.md](../HU-038.md) → `@2-tester-spec` → `@3-implementador` →
validación del responsable: `mvnw clean verify` con Docker real (**1014 pruebas, 0 fallos, 2 omitidas**, JaCoCo), `consistencia.ps1`
CONSISTENTE, `drift.ps1` SIN DERIVA, y E2E contra el stack edge reconstruido con este código (WAF → PDP → OPA real → SurrealDB → Redis).

## Qué cambia

| Síntoma original | Resultado |
|---|---|
| Un usuario válido en Keycloak que nunca entró al panel recibía 401 por Bearer | El decoder Bearer resuelve —o crea de forma idempotente— el usuario y la identidad del `sub` en el tenant del claim, **antes** de consultar la revocación |
| `POST /api/v1/authorize` por Bearer devolvía `DENY / NO_APPLICABLE_POLICY` siempre | El principal lleva el `UserId`; roles y derechos se resuelven y la cadena asignar → autorizar llega a `ALLOW` |

## Criterios

| # | Criterio | Resultado |
|---|---|---|
| C1 | Primer uso Bearer de un sujeto desconocido crea usuario + identidad en el tenant del claim | ✅ `BearerAuthorizationHttpTests` (Surreal real) |
| C2 | Tenant inexistente ⇒ 401 uniforme y no se crea nada | ✅ |
| C3 | `authorize` por Bearer: `DENY` antes de asignar el rol, `ALLOW` después; el `input` a OPA lleva los roles | ✅ HTTP y E2E |
| C4 | Un claim interno `pdp_user_id` forjado en el token no suplanta a nadie (siempre se sobrescribe o se elimina) | ✅ |
| C5 | Correo de otra cuenta ⇒ 401, sin vincular por correo; `email_verified` o `{sub}@bearer.invalid` | ✅ unitarias |
| C6 | Fallo de resolución ⇒ fail-closed 401; un `Error` de la JVM no se disfraza | ✅ |
| C7 | Perfil HMAC (desarrollo): sellado suave, sin aprovisionar | ✅ |
| C8 | Arquitectura, consistencia, drift y `verify` completo | ✅ |

## E2E contra el stack real

- `host-authorize.hurl` (Bearer: administrar → asignar → `DENY` → `ALLOW`): **9/9 peticiones, ✅**. Antes de HU-038 fallaba en el `ALLOW` final.
- `host-bearer.hurl` pasa hasta el paso 5. El paso 6 (`e2e-nonadmin` ⇒ 403 al definir catálogo TENANT) devuelve 201 **porque esta rama no
  incluye HU-036** (PR #84, rama distinta): el gate no existe en la imagen probada. No es una regresión de HU-038; se repite al integrar ambas.
- `host-authorize-cookie.hurl` devolvió 409 al repetirlo: registra un nombre fijo y no es re-ejecutable sobre una base ya usada
  (el flujo por cookie se verificó en la corrida anterior: `DENY` → `ALLOW`).

## Límites conocidos

- Se asume **un solo emisor** por despliegue (la identidad se guarda con su `iss`).
- La transacción `BEGIN/COMMIT` de `createUserWithIdentityIfAbsent` está cubierta por `SurrealSecurityUserRepositoryProvisioningTests`
  contra un SurrealDB de Testcontainers; no contra la versión desplegada en producción.
- Pendiente tras fusionar HU-036 y HU-038: repetir `host-bearer.hurl` completo (sin `first-login`) sobre la rama integrada.

## Cambios de comportamiento (deliberados)

Un Bearer válido de un sujeto nuevo ahora **provisiona** su usuario en el primer uso (antes: 401). Un token con tenant desconocido sigue siendo 401.
