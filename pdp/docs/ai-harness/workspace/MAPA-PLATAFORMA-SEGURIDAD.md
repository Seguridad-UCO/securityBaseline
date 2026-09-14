# Mapa de la Plataforma de Seguridad — estado real vs. lista de la reunión

> **Estado: 2026-09-13 (noche)**, verificado contra `develop` archivo por archivo (no de memoria) —
> `mvnw clean verify`: **653 pruebas, 0 fallos, 0 errores**, cobertura y arquitectura en verde.
> Origen: una lista de 21 puntos y ~67 pendientes tomada en una reunión con Farid, sobre
> observabilidad, PEP/PDP/OPA, librería de integración, microfrontend de seguridad y administración
> por aplicación. Este documento la limpia: qué ya existe, qué es parcial, qué no ha empezado, y en
> qué orden seguir.
>
> Versión visual (mapa conceptual con diagrama del flujo hoy/objetivo, misma información):
> https://claude.ai/code/artifact/fe85f5ae-f026-4c3e-b1d9-1e4afc548dfc

**Balance: 31 hechos y verificados · 12 parciales · 24 sin empezar**, de 67 ítems.

---

## 1. El mecanismo — qué corre hoy y qué solo está acordado

La cadena completa **PEP ↔ PDP ↔ OPA** corre de punta a punta y queda auditada. El PDP además ya
tiene su propio catálogo de aplicaciones con credencial (emitida, validada y rotable) y con
administración gateada — el eslabón que le faltaba para dejar de depender de que un humano reparta
tokens y decida a mano quién puede tocar cada aplicación.

```
Aplicación integrada --alta técnica propia (no migrada)--> PEP --mTLS, corre--> PDP --HTTP, corre--> OPA
                                                                                   │
                                                                  SPA --cookie--> PDP --lee--> SurrealDB
                                                                                   │
                                                                              autentica
                                                                                   │
                                                                               Keycloak
```

Lo único que sigue sin ser código de conexión: OPA responde siempre `DENY`/`NO_APPLICABLE_POLICY`
porque `allow_candidates`/`deny_candidates` en `composition.rego` siguen vacíos — nadie ha publicado
todavía una política de aplicación real (trabajo de Laura, `security-policy-engine/`). Y el PEP
sigue dando de alta cada integración con su propio token manual en `integrations.json`, sin
consultar la credencial que el PDP ya sabe emitir, validar y rotar.

---

## 2. Estado por capacidad

| Capacidad | Hecho | Parcial | Pendiente | Resumen |
|---|---|---|---|---|
| Arquitectura y contratos | 6 | 1 | 1 | Sin cambios recientes. `contracts/` con OpenAPI + JSON Schema, ejemplos validados en el build |
| Seguridad PEP · PDP · OPA | 5 | 3 | 3 | Los tres saltos de código corren y cada decisión queda auditada. Falta la política de aplicación en OPA y migrar el PEP a la credencial del PDP |
| Librería de integración | 5 | 4 | 0 | El PDP ya emite (HU-012), valida (HU-013) y rota (HU-014) una credencial de aplicación. El starter del PEP (v0) sigue sin consumirla |
| Observabilidad | 13 | 3 | 3 | Todo el stack (OTel, Jaeger, Loki, Prometheus, Grafana) armado y cableado, con auditoría de decisiones real (HU-007). Falta encenderlo con tráfico real y documentar la convención |
| Microfrontend de seguridad | 0 | 1 | 11 | Ya no bloqueado por falta de datos de dominio (roles, perfiles, asignaciones, credenciales existen). Sigue sin arrancar como proyecto y sin el ADR de administración |
| Administración por aplicación | 2 | 1 | 5 | **HU-015**: el registro ya da de alta al primer administrador automáticamente, y borrar/rotar la credencial de una aplicación exige serlo (gateado vía OPA, mecanismo de HU-009). Falta el resto: delegar, listar/quitar administradores por HTTP público, y el ADR formal de "rol global vs. por aplicación" |

---

## 3. Qué cambió en este corte — HU-014 y HU-015

**HU-014 — Rotación de credencial de aplicación.** `RotateApplicationCredentialUseCase` (slice
`applications`): genera un secreto nuevo, lo hashea y reemplaza el anterior sin re-registrar la
aplicación entera — cerraba la deuda de que perder un secreto obligaba a recrear el recurso.

**HU-015 — Administración del catálogo de aplicaciones.** Dos piezas:

1. **Alta automática del primer administrador.** Registrar una aplicación (`POST
   /api/v1/applications`, ahora orquestado desde `assignments`, no desde `applications`) ya no
   termina en un catálogo sin dueño: se define un rol `ADMIN` con alcance a esa aplicación y se
   asigna de inmediato a quien la registró. Nadie tiene que pedir el rol después.
2. **Borrar y rotar exigen ser administrador.** `RemoveApplicationUseCase` y
   `RotateApplicationCredentialUseCase` (HU-014) quedaron gateados: ambos se movieron a orquestarse
   desde `authorization`, que valida contra OPA (mecanismo de HU-009, no un `if` de rol en Java)
   que quien llama administra esa aplicación antes de delegar en el caso de uso real.
3. **Backfill explícito.** Para aplicaciones que ya existían antes de HU-015 y no tienen
   administrador, un endpoint interno (`InternalApplicationAdministratorController`, mTLS) permite
   asignarlo a mano — sin gate automático porque no hubo un "quien registra" que capturar.

Durante la verificación de esta historia en `develop` apareció un **bug de producción real, no
relacionado con HU-015**: el índice único `role_scope_name`/`profile_scope_name` sobre
`(level, tenantId, applicationId, name)` no indexaba ni aplicaba unicidad cuando `applicationId`
está ausente (roles/perfiles de alcance `TENANT`/`GLOBAL`) — SurrealDB no compara `NONE` contra un
campo ausente vía índice, así que dos roles idénticos de alcance tenant podían coexistir sin que el
índice lo impidiera, y `existsByNameInScope`/`findByNameInScope` siempre devolvían falso/vacío para
ese alcance. Corregido reemplazando el centinela de "ausente" por una cadena vacía (un valor real e
indexable) en `SurrealRoleRepository`/`SurrealProfileRepository`. Confirmado reproduciendo el caso
exacto contra una instancia de SurrealDB antes y después del fix.

---

## 4. Pendientes por prioridad — recalculado tras HU-014/HU-015

### P1 — cierra el ciclo de la credencial

Migrar `pep/starter` para que su alta técnica consuma la credencial que el PDP ya emite, valida y
rota, en vez de (o además de) su token manual repartido a mano. Historia del lado del PEP, no del
PDP — HU-012/013/014 la dejaron fuera de alcance a propósito.

### P0 — desbloquea el primer `ALLOW` real

Una política de aplicación real en `policies/applications/`. `allow_candidates`/`deny_candidates`
son *extension points* a propósito, vacíos hasta que alguien publique un módulo bajo ese paquete
(Laura, `security-policy-engine/`). No es una historia del PDP.

### P2 — cerrar lo que ya se armó (observabilidad)

El código y el compose ya existen y la auditoría de decisiones ya no falta. Falta verificarlo en
ejecución conjunta, escribir la convención de telemetría, exponer `access_event` a la pila de
observabilidad, y mover el perfil `observability` de opt-in a default.

### P3 — decidir antes de construir

Microfrontend de seguridad. Ya no bloqueado por falta de datos de dominio — sí por decisión y por
arrancar el proyecto.

**Ya no está bloqueado por decisión el resto de administración por aplicación** — se resolvió con
`ADR-023-application-administration-model.md` y `ADR-024-global-application-administrator.md`
(`security-platform-architecture`), y quedó un backlog ordenado listo para planificarse con el harness
de agentes:

| # | Historia | Qué cierra |
|---|---|---|
| HU-016 | Gatear roles (`DefineRole`, `GrantResourceToRole`) | Brecha de mínimo privilegio — prioridad #1 |
| HU-017 | Gatear registro de recursos protegidos | Misma brecha, slice `resources` |
| HU-018 | Gatear asignación/revocación de roles | Misma brecha, slice `assignments` |
| HU-019 | Gatear perfiles (definir, componer, asignar) | Misma brecha, slices `profiles`+`assignments` |
| HU-020 | Autoservicio de administradores (agregar/quitar/listar por HTTP público) | Reemplaza la dependencia del canal interno mTLS para uso cotidiano |
| HU-021 | Auditoría de operaciones administrativas (evento propio) | Deuda aplazada dos veces (HU-009, HU-015) |

Administrador global (`ADR-024`): decisión de diseño tomada y documentada, construcción diferida
hasta que exista un caso de uso concreto (candidato: abrir `POST /api/v1/roles` para alcance
`GLOBAL`, hoy en `400` a propósito) — no es una historia de este backlog.

### P4 — evaluar, no construir

Zero Trust como postura escrita, MFA (config de Keycloak), cache distribuida, serverless (no
evaluado).

---

## 5. Hallazgos de este corte

**El eslabón de la credencial ya existe de un lado, sigue sin existir del otro.** El PDP ya emite,
valida y rota una credencial de aplicación (HU-012/013/014). El PEP sigue sin preguntarle — su alta
técnica no cambió.

**"Quién administra" ya es una decisión de OPA, no de Java.** El gate de HU-015 en Remove/Rotate no
es un `if (rol == ADMIN)` dentro del caso de uso: valida contra el mecanismo de HU-009, que a su vez
resuelve la decisión vía OPA. Es la misma regla que ya regía para autorización de negocio, aplicada
ahora a operaciones administrativas.

**Un índice único puede pasar una verificación de tests y aun así no funcionar en el caso que nunca
se ejercitó.** El bug de `role_scope_name`/`profile_scope_name` con `applicationId` ausente llevaba
desde que el índice se definió (antes de HU-015) sin que ningún test tocara un rol o perfil de
alcance tenant/global después de que el índice existiera — todos los casos existentes usaban alcance
`APPLICATION`, donde el campo nunca está ausente. Ver §3.

**Criterio 10 (transacciones/saga) sigue siendo el único no cumplido.** `RemoveApplicationUseCase`
existe como compensación (ahora gateada por HU-015) y ningún caso de uso automático la invoca fuera
de la compensación manual de HU-010.

---

## 6. La lista original, ítem por ítem

### A · Observabilidad

Sin cambios desde el corte de HU-013: **13 hechos, 3 parciales, 3 pendientes**. Ver la versión
visual para el detalle fila por fila (Correlation ID, timestamps, trazas OTel/Jaeger, logs
JSON/Loki, métricas Prometheus, dashboard Grafana, auditoría de decisiones vía `AccessEvent`).

### B · Seguridad

Sin cambios en la cadena PEP↔PDP↔OPA desde HU-013. Lo que sí cambió: **manejo de claves** pasa a
incluir rotación (HU-014) y **auditoría de operaciones de seguridad** — la autorización de negocio
ya se auditaba (HU-007); la administración del catálogo (crear/borrar/rotar aplicación) todavía no
tiene su propio evento de auditoría, solo el rechazo o éxito de la operación en sí.

### C · Librería de integración

Sin cambios: el PDP ya cubre credenciales (emitir, validar, rotar); el starter del PEP sigue en v0
y no las consume (P1).

### D · Arquitectura

Sin cambios. HU-014/HU-015 no declaran ninguna frontera de Modulith nueva sin justificar: los
`allowedDependencies` que se ampliaron (`applications :: model`, `roles :: model`/`usecase`,
`identity :: rule` en `authorization`; `roles :: dto` sobre `primaryport/response`) están
documentados en `PLAN-HU-015.md` §0/§13/§14.

### E · Microfrontend de seguridad

Sin cambios de código. Ya no bloqueado por falta de datos — roles, perfiles, asignaciones y ahora
administración por aplicación existen. Sigue bloqueado por el ADR de administración y por arrancar
el proyecto (`securityBaseline-fr` sigue en dos archivos fuente).

### F · Administración de seguridad por aplicación

| Estado | Ítem | Evidencia / qué falta |
|---|---|---|
| ✅ | Alta automática del primer administrador | HU-015: `RegisterApplicationWithFirstAdministratorUseCaseImpl` asigna el rol `ADMIN` de alcance-aplicación al registrador, en la misma operación de registro |
| ✅ | Borrar/rotar credencial exige ser administrador | HU-015: `AdministerApplicationRemovalUseCaseImpl`/`AdministerApplicationCredentialRotationUseCaseImpl` gatean vía el mecanismo de HU-009 (OPA), no un `if` de rol |
| 🟡 | Backfill de administrador para aplicaciones preexistentes | `InternalApplicationAdministratorController` (mTLS) existe; es manual, sin flujo de descubrimiento de "aplicaciones sin administrador" |
| ⬜ | Delegar administración a otro usuario | No modelado |
| ⬜ | Listar/quitar administradores por HTTP público | No existe endpoint |
| ⬜ | ADR formal: rol global vs. por aplicación vs. perfil | Sigue sin escribirse — HU-015 resolvió el caso concreto (aplicación) sin cerrar la decisión general |
| ⬜ | Auditoría de operaciones administrativas | Las operaciones de HU-015 no emiten `AccessEvent` propio, solo la respuesta HTTP |
| ⬜ | Panel/UI de administración | Depende del microfrontend (sección E) |

---

## 7. El guion de la reunión, con lo que es real marcado

1. **Problema** — las aplicaciones necesitan seguridad común sin implementarla cada una. ✅ se
   sostiene.
2. **Arquitectura** — PEP, PDP y OPA independientes. ✅ hecho, y el primer salto entre ellos corre de
   punta a punta.
3. **Librería** — la aplicación instala un starter y configura. 🟡 el PDP ya sabe emitir, validar y
   rotar la credencial; el starter todavía no la consume.
4. **Microfrontend** — gestión de seguridad centralizada. ⬜ pendiente, ya no bloqueado por datos,
   bloqueado por decisión y por arrancar el proyecto.
5. **Modelo de dominio** — aplicaciones, roles, usuarios, permisos. ✅ hecho en su mayoría: tenants,
   aplicaciones (con credencial y administración propia desde HU-012/015), recursos, usuarios,
   roles, asignaciones y perfiles existen y están probados (653 pruebas).
6. **Administradores** — quién administra la seguridad de cada aplicación. 🟡 resuelto para el caso
   concreto de una aplicación (HU-015, vía OPA); sigue sin ADR general ni delegación.
7. **Observabilidad** — Correlation ID, trazas, logs, métricas, auditoría. ✅ config completa +
   auditoría real de decisiones de acceso. Falta encender el stack con tráfico real.
8. **Resultado esperado** — registrar la app → credencial → administrador → librería → PEP → PDP →
   roles/perfiles → OPA → decisión → auditoría. Toda la cadena de código corre con datos reales y
   queda registrada, salvo que la librería del PEP todavía usa su propia credencial y OPA todavía no
   tiene una política de aplicación que evaluar.
