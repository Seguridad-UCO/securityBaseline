# Mapa de la Plataforma de Seguridad — estado real vs. lista de la reunión

> **Estado: 2026-09-11**, verificado contra `main` archivo por archivo (no de memoria). Origen: una
> lista de 21 puntos y ~67 pendientes tomada en una reunión con Farid, sobre observabilidad,
> PEP/PDP/OPA, librería de integración, microfrontend de seguridad y administración por aplicación.
> Este documento la limpia: qué ya existe, qué es parcial, qué no ha empezado, y en qué orden seguir.
>
> Versión visual (mapa conceptual con diagrama del flujo hoy/objetivo):
> https://claude.ai/code/artifact/fe85f5ae-f026-4c3e-b1d9-1e4afc548dfc

**Balance: 15 hechos y verificados · 19 parciales · 33 sin empezar**, de 67 ítems.

---

## 1. El mecanismo — qué corre hoy y qué solo está acordado

Hoy el PEP y OPA son dos islas: cada uno está implementado y probado, pero contra simuladores. Lo
único que corre de punta a punta es la columna del panel (`SPA → PDP → SurrealDB`, vía
`POST /api/v1/authorize`, HU-002).

```
Aplicación integrada --alta técnica--> PEP --[HU-003, contrato acordado]--> PDP --[HU-005, contrato acordado]--> OPA
                                                                               │
                                                              SPA --cookie--> PDP (corre hoy) --lee--> SurrealDB
                                                                               │
                                                                          autentica
                                                                               │
                                                                           Keycloak
```

Los dos saltos marcados `[contrato acordado]` ya tienen su schema versionado en `contracts/` tras
las cuatro decisiones de unificación del 2026-09-10. Falta el código que los conecte — eso es
exactamente HU-003 y HU-005.

---

## 2. Estado por capacidad

| Capacidad | Hecho | Parcial | Pendiente | Resumen |
|---|---|---|---|---|
| Arquitectura y contratos | 6 | 1 | 1 | Contract First ya es práctica: `contracts/` con OpenAPI + JSON Schema, regla de "acordar antes de implementar", ejemplos validados en el build |
| Seguridad PEP · PDP · OPA | 2 | 5 | 4 | Tres componentes de verdad independientes (tres árboles, tres builds, cero código compartido). Los contratos existen; los dos saltos de código no |
| Librería de integración | 3 | 5 | 1 | `pep/starter` existe y cumple lo que pide la lista para el alta técnica — pero no protege endpoints, y su credencial no la emite el PDP |
| Observabilidad | 4 | 7 | 8 | Correlation ID, ISO 8601, logger y filtro HTTP hechos. El puente OTel está en el POM pero no exporta a ningún lado. Prometheus/Loki/Jaeger/Grafana no existen |
| Microfrontend de seguridad | 0 | 1 | 11 | No empezado, y hoy no tendría nada que administrar: no existen roles (HU-004) ni perfiles (HU-007) |
| Administración por aplicación | 0 | 0 | 8 | No modelado. Depende de que existan roles y de resolver quién emite la credencial de una aplicación |

---

## 3. Pendientes por prioridad — el orden es de dependencias

### P0 — HU-003: endpoint interno `POST /internal/v1/access-decisions`

Es el único pendiente que tiene a alguien esperando: el PEP de David está terminado y probado
contra un simulador. Las diez decisiones de diseño ya están tomadas y documentadas
(`HANDOFF-INTEGRACION-PEP-OPA.md`) — mTLS con `client-auth=want` + filtro que falla cerrado, tenant
resuelto desde el catálogo, evidencia JWT contra un conjunto de confianza. **Ya tiene plan y
esqueletos** (rama `feature/HU-003-endpoint-interno-pep`) — ver `CHECKPOINT` de esa historia más
abajo para el siguiente paso concreto.

### P1 — la cadena HU-004 → HU-005 → HU-006

1. **HU-004 — roles y asignaciones vigentes por tenant.** Sin atributos que evaluar, conectar OPA
   no cambia nada: seguiría respondiendo `NO_APPLICABLE_POLICY`. Es el trabajo más grande y 100 %
   del PDP. También es lo que le da al microfrontend algo que gestionar.
2. **HU-005 — `OpaPolicyDecisionAdapter`.** Sustituye la denegación por defecto por una decisión
   real. Con esto el camino `ALLOW` deja de ser matemáticamente imposible.
3. **HU-006 — `EventoAcceso` hacia auditoría.** Cierra de una vez cuatro ítems de la lista: eventos
   de auditoría, trazabilidad de decisiones, auditoría de operaciones administrativas, y la decisión
   de qué va en segundo plano (outbox o bloqueante).

### P2 — observabilidad y librería v1 (en paralelo, no dependen de P1)

1. **Exportador OTel + Jaeger.** El puente ya está en el POM con sampling al 100 %; hoy las trazas
   se generan y se tiran. Añadir el exportador OTLP y un Jaeger en el compose es el cambio más
   barato con más retorno de toda la lista.
2. **Convención de telemetría escrita** (nombres de atributos, servicio, operación, error) — un
   documento, antes de tocar más código, para que el PEP y el PDP no diverjan otra vez como pasó con
   los contratos.
3. **Logs JSON con esa convención + Loki.** El patrón actual ya es clave=valor con ISO 8601; pasarlo
   a JSON es mecánico. Falta añadir `traceId`/`spanId`, servicio y ambiente.
4. **Registro Prometheus + Grafana.** Actuator ya expone `metrics`; falta el registry y el compose
   de observabilidad.
5. **Librería v1:** que el PDP emita la credencial al registrar la aplicación y el starter la use —
   hoy hay dos registros desconectados (ver hallazgo 1). Depende de una decisión de dominio.

### P3 — decidir antes de construir: microfrontend, administración por aplicación

Son los puntos que Farid señaló como más importantes, y lo son estratégicamente — pero técnicamente
están bloqueados por P1: un microfrontend de seguridad sin roles ni perfiles es una pantalla vacía.
Lo que sí se puede hacer ahora es **decidir**:

- El modelo de dominio de "qué administra cada aplicación" y "quién puede administrarla": ¿rol
  global, rol por aplicación, delegación? Entra al repo de arquitectura como ADR, no como código.
- Cómo el microfrontend identifica la aplicación que lo invoca (mismo problema que la credencial de
  la librería — conviene resolverlos juntos).
- Qué información es del ecosistema y cuál de cada aplicación.
- Con eso decidido, la POC se construye después de HU-004 con datos reales.

### P4 — evaluar, no construir

- **Zero Trust** ya se practica (tenant del principal, PEP falla cerrado, mTLS decidido, nada por
  cabecera) — falta escribirlo como postura en un documento. Barato, da lenguaje común.
- **MFA** es configuración de Keycloak, no código nuestro. Se decide cuándo, no cómo.
- **Cache distribuida** solo tiene sentido cuando existan roles que cachear (después de HU-004). El
  único gancho hoy es que se valida `jti` "para una futura revocación por Redis".
- **Serverless** no se ha evaluado. Hoy todo corre como contenedor en App Service; la lista pide
  justificarlo por costo/latencia, no adoptarlo por moda.

---

## 4. Tres hallazgos que la lista no sabía

**Hay dos registros de aplicaciones, y no se hablan.** El PDP tiene su catálogo
(`pdp/applications`, en SurrealDB). El PEP tiene el suyo (`RouteRegistry`, con su propia credencial
por integración). El starter se registra en el segundo. La "clave privada generada por el PDP al
crear la aplicación" que pide la lista es exactamente el eslabón que uniría los dos — y hoy no
existe. Es una decisión de dominio que toca al PDP, al PEP y a la librería.

**Las trazas se generan y se tiran.** `micrometer-tracing-bridge-otel` está en el POM del PDP con
muestreo al 100 %, pero no hay exportador ni destino. Añadir uno es el cambio más pequeño con más
impacto de toda la sección de observabilidad.

**La librería "deliberately installs no HTTP filter".** Textual del código del starter. Hace el
alta técnica y nada más: la protección de los endpoints de la aplicación sigue recayendo en el PEP
como proxy, no en la librería. Es una decisión de arquitectura razonable (el enforcement vive en un
solo sitio), pero la lista de la reunión asume lo contrario — conviene aclararlo con Farid antes de
que lo pida.

---

## 5. La lista original, ítem por ítem

### A · Observabilidad

| Estado | Ítem | Evidencia / qué falta |
|---|---|---|
| ✅ | Correlation ID | `shared/web/CorrelationWebFilter`: genera o propaga `X-Request-Id`/`X-Correlation-Id`, los devuelve en la respuesta, los publica en el contexto de Reactor y en el MDC del log. El PEP los envía por contrato |
| ✅ | Timestamps ISO 8601 | `%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX}` en `application.properties` |
| ✅ | Nombre estándar de logger | `logger=%logger{36}` |
| ✅ | Filtros HTTP / interceptores | `CorrelationWebFilter` (PDP, primero de la cadena) e `IngressLimitsFilter` (PEP) |
| 🟡 | Estrategia de trazabilidad distribuida | El eslabón PEP → PDP ya propaga los IDs por contrato. Falta el frontend, y falta escribirla como estrategia |
| 🟡 | Trace ID y Span ID | `micrometer-tracing-bridge-otel` en el POM, `sampling.probability=1.0`. Sin exportador; no aparecen en el patrón de log |
| 🟡 | Estructura estándar de logs | Clave=valor en el PDP. No JSON, no convención OTel, sin servicio ni ambiente. El PEP tiene su propio formato |
| 🟡 | Eventos de error | Un solo `ApiErrorHandler` (`@RestControllerAdvice`) da forma uniforme a las respuestas. No hay evento de log definido |
| 🟡 | Eventos de procesos de negocio | `DomainEventPublisher` + eventos de dominio existen (in-process, Spring). No se emiten como telemetría |
| 🟡 | Integrar OpenTelemetry | Solo el puente. Sin `opentelemetry-exporter-otlp` |
| 🟡 | Reconstruir una transacción por Correlation ID | Posible dentro del PDP con grep sobre el log. No hay almacén centralizado |
| ⬜ | Eventos de auditoría | → HU-006 |
| ⬜ | Identificar eventos críticos | Parte de la convención de telemetría (P2.2) |
| ⬜ | Convenciones semánticas OTel | P2.2 |
| ⬜ | Librería transversal de instrumentación | El starter del PEP no instala filtros a propósito |
| ⬜ | Prometheus | Actuator expone `metrics`; sin `micrometer-registry-prometheus` |
| ⬜ | Loki | — |
| ⬜ | Jaeger | — |
| ⬜ | Grafana | — |

### B · Seguridad

| Estado | Ítem | Evidencia / qué falta |
|---|---|---|
| ✅ | Documentar arquitectura PEP/PDP/OPA | `docs/PLATAFORMA.md`, `contracts/README.md` |
| ✅ | PEP, PDP y OPA independientes | Tres árboles, tres builds, cero código compartido. Solo se tocan en `contracts/` |
| 🟡 | Integración Aplicación → PEP | El starter hace el alta. No protege endpoints locales |
| 🟡 | Integración PEP → PDP | Contrato `contracts/pep-pdp/v1` acordado. Endpoint no existe → HU-003 |
| 🟡 | Integración PDP → OPA | Contrato `contracts/pdp-opa/v1` acordado. Adaptador no existe → HU-005 |
| 🟡 | Zero Trust | Se practica (ADR-018, fallo cerrado, mTLS decidido, nada por cabecera). No está escrito como postura |
| 🟡 | Manejo de claves | Secretos de despliegue por Key Vault y service connections. Claves de aplicación: no existen |
| ⬜ | MFA | Capacidad de Keycloak, no configurada ni decidida |
| ⬜ | Auditoría de operaciones de seguridad | → HU-006 |
| ⬜ | Cache distribuida | Único gancho: `jti` validado "para una futura revocación por Redis". Después de HU-004 |
| ⬜ | Invalidación / actualización de seguridad | Depende de la cache |

### C · Librería

| Estado | Ítem | Evidencia / qué falta |
|---|---|---|
| ✅ | Agregarla como dependencia | `pep/starter/pom.xml` + `AutoConfiguration.imports`: es un starter de Spring Boot de verdad |
| ✅ | Configurar desde `application.properties` | `security.pep.registration.*` (`PepRegistrationProperties`) |
| ✅ | URL del PEP | `security.pep.registration.pep-url`, HTTPS obligatorio salvo `allow-insecure-http` |
| 🟡 | Crear librería transversal | v0: solo registro técnico, con validación y reintentos con backoff |
| 🟡 | Credenciales / claves | Un `token` Bearer — pero lo emite el registro del PEP, no el PDP |
| 🟡 | Automatizar configuración | Validación al arrancar; sin descubrimiento |
| 🟡 | Evitar implementación manual | Cubre el alta. El enforcement lo hace el PEP como proxy, no la librería |
| 🟡 | Ejecución en segundo plano | `DomainEventPublisher` in-process. Decisión sobre auditoría async (outbox) → HU-006 |
| ⬜ | Clave privada generada por el PDP | El PDP registra aplicaciones pero no emite credenciales. Ver hallazgo 1 |

### D · Arquitectura

| Estado | Ítem | Evidencia / qué falta |
|---|---|---|
| ✅ | API First | `contracts/pep-pdp/v1/openapi.yaml` antes que el código |
| ✅ | Contract First | Las cuatro decisiones de unificación (D-U1..D-U4) son la prueba |
| ✅ | Mapper | Criterios 13 y 14: DTO crudo → mapper con `RequestFieldParser` → record con VOs y `requireNonNull` |
| ✅ | Domain Context | Modulith con `allowedDependencies` explícitos; `domain/{slice}/{model,rule,exception,message}` |
| ✅ | Separación dominio / API / infraestructura | `LayeredArchitectureTests` + `ModulithStructureTests` |
| ✅ | Componentes transversales del ecosistema | Seguridad: sí (`contracts/`, `PLATAFORMA.md`) |
| 🟡 | Información transversal (parámetros, mensajes, notificaciones) | Solo seguridad está resuelta como transversal |
| ⬜ | Serverless | No evaluado. Hoy: contenedor en App Service |

### E · Microfrontend de seguridad

| Estado | Ítem | Evidencia / qué falta |
|---|---|---|
| 🟡 | Modelar la relación en el dominio | Existen `tenants`, `applications`, `resources`, `identity`. Roles → HU-004, perfiles → HU-007. "Qué administra cada aplicación" no está modelado |
| ⬜ | POC del microfrontend y el resto de ítems | Nada. `securityBaseline-fr` tiene dos archivos fuente. Bloqueado por HU-004 en lo técnico; las decisiones (P3) sí se pueden tomar ya |

### F · Administración de seguridad por aplicación

| Estado | Ítem | Evidencia / qué falta |
|---|---|---|
| ⬜ | Los 8 ítems | No modelado. Requiere HU-004, una decisión de dominio (ADR: rol global vs por aplicación, delegación), y HU-006 para auditoría. La regla "ni un solo `if (rol == ...)`" aplica también aquí: quién puede administrar es una política de OPA, no una rama en Java |

---

## 6. El guion de la reunión, con lo que es real marcado

1. **Problema** — las aplicaciones necesitan seguridad común sin implementarla cada una. ✅ se sostiene.
2. **Arquitectura** — PEP, PDP y OPA independientes. ✅ hecho.
3. **Librería** — la aplicación instala un starter y configura. 🟡 existe y hace el alta; el
   enforcement lo hace el PEP como proxy, no la librería. Decirlo explícitamente evita una
   expectativa equivocada.
4. **Microfrontend** — gestión de seguridad centralizada. ⬜ pendiente, y bloqueado por el modelo de
   dominio del paso siguiente. Presentarlo como diseño, no como POC.
5. **Modelo de dominio** — aplicaciones, roles, usuarios, permisos. 🟡 tenants/aplicaciones/recursos/
   usuarios existen; roles y asignaciones son HU-004.
6. **Administradores** — quién administra la seguridad de cada aplicación. ⬜ decisión abierta;
   propuesta: se decide como política de OPA, no como rol en Java.
7. **Observabilidad** — Correlation ID, trazas, logs, métricas. 🟡 el Correlation ID ya cuenta la
   historia dentro del PDP; el resto del stack no existe. El primer paso barato es exportar las
   trazas que ya se generan.
8. **Resultado esperado** — registrar la app → parámetros → librería → PEP → microfrontend → roles →
   observabilidad. Hoy funcionan los pasos 1, 3 y 4 (registro en el PEP, no en el PDP). Unir los dos
   registros es lo que convierte la lista en un flujo real.
