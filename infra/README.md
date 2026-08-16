# Infraestructura: Azure Key Vault y manejo de secretos

## Estado actual, sin adornos

La aplicación tiene dos secretos reales: la clave HMAC que firma y valida los JWT del emisor propio
(ADR-0003, `pdp.security.jwt.secret`) y la contraseña de SurrealDB (ADR-0004,
`pdp.persistence.surrealdb.password`). El exportador de telemetría remoto sigue sin existir — es el
único de los tres subsistemas con secreto reservado que aún no consume su variable de entorno.

```bash
git grep -nIE "(password|secret|api[_-]?key|token|credential)" -- . ':!docs' ':!*.md'
```

Esa búsqueda ahora sí encuentra algo real: `pdp.security.jwt.secret=dev-only-signing-key-...` en
`application.properties`. No es una fuga — el valor está deliberadamente marcado como
`dev-only-signing-key-not-for-production-use`, y tanto `application-dev.properties` como
`application-prod.properties` lo vacían para usar JWKS contra Keycloak real en su lugar (ADR-020) —
ningún ambiente desplegado hereda la clave de desarrollo.

Lo que sigue sin existir es el exportador de telemetría; su mecanismo de secreto (Key Vault,
`${PDP_OTLP_TOKEN}`) está preparado desde antes de que el subsistema exista, siguiendo el mismo
patrón que los secretos de JWT y SurrealDB ya usan en producción.

## Qué es configuración y qué es secreto

No todo lo externalizable es sensible, y tratar todo como secreto hace que el vault deje de
significar algo.

| Valor | Dónde vive | Por qué |
|---|---|---|
| `pdp.tenants.seed[*]` | `application.properties` | Identificadores de tenant; públicos por diseño |
| `pdp.applications.reserved-names` | `application.properties` | Política de nombres; no confidencial |
| `management.endpoints.*` | `application.properties` | Configuración operativa |
| Nombres de App Service, resource group, Key Vault | `ci/variables/*.yml` | Identificadores de recursos, no credenciales |
| Contraseña de SurrealDB | **Key Vault** | Da acceso de lectura y escritura a los datos |
| Clave de firma JWT (emisor propio) | **Key Vault** | Permite falsificar cualquier token |
| Client secret de Keycloak/OIDC | **Key Vault** | Permite suplantar a la aplicación |
| Token del colector OTLP | **Key Vault** | Permite inyectar o leer telemetría |

## Secretos reservados en el vault

Estos son los nombres reservados para cuando su subsistema exista. `stage-deploy.yml` solo pide y
consume `pdp-datasource-password` hoy — es el único que un
ambiente desplegado necesita para arrancar hoy (`application-dev.properties` y
`application-prod.properties` lo exigen sin valor de respaldo, junto con
`PDP_DATASOURCE_URL`/`PDP_DATASOURCE_USERNAME`, que son configuración operativa, no secretos). El
`SecretsFilter` del pipeline se restringe deliberadamente a eso: pedir un secreto que no existe en
el vault hace fallar la tarea `AzureKeyVault@2` completa, así que la fila queda tan corta como lo
que realmente se usa.

| Secreto | Consumidor previsto | Variable de entorno | Estado |
|---|---|---|---|
| `pdp-datasource-password` | Adaptador SurrealDB (ADR-0004) | `PDP_DATASOURCE_PASSWORD` | **En uso** |
| `pdp-jwt-signing-key` | Emisor/validador JWT propio (ADR-0003) | `PDP_JWT_SIGNING_KEY` | Huérfano desde ADR-020 — DEV y PROD validan vía JWKS contra Keycloak real, no HMAC. El modo HMAC sigue vivo solo en el perfil por defecto (tests, desarrollo local), que no lee este secreto |
| `pdp-oidc-client-secret` | Integración con Keycloak (ADR-0003) | `PDP_OIDC_CLIENT_SECRET` | Reservado — ningún `application-*.properties` lo referencia |
| `pdp-otlp-token` | Exportación de trazas | `PDP_OTLP_TOKEN` | Reservado — el subsistema de telemetría no existe todavía |

## Cómo se consumen

```text
Azure Key Vault
      │  (RBAC: Key Vault Secrets User)
      ├── service principal del pipeline ──► tarea AzureKeyVault@2
      │                                          │ variables enmascaradas
      │                                          ▼
      │                              az webapp config appsettings set ─► app settings
      │                                                          │
      └── identidad administrada de la app ◄─────────────────────┘
                                                                 ▼
                                                    variables de entorno del proceso
                                                                 ▼
                                              ${PDP_DATASOURCE_PASSWORD} en Spring
```

Dos consumidores, dos motivos:

- **El pipeline** los lee para inyectarlos como *app settings* durante el despliegue.
- **La aplicación** tiene su propia identidad administrada con acceso de lectura, de modo que
  cuando se adopte `spring-cloud-azure-starter-keyvault-secrets` pueda resolverlos en arranque sin
  que el pipeline los toque nunca. Se dejó preparada esa ruta pero no se activó: añadir hoy la
  dependencia significaría cargar un starter que no tiene ningún secreto que resolver.

En el código, un secreto siempre se referencia por marcador de posición, nunca por valor:

```properties
spring.datasource.password=${PDP_DATASOURCE_PASSWORD}
```

## Decisiones del vault y por qué

- **`enableRbacAuthorization: true`** — las políticas de acceso clásicas no aparecen en las
  revisiones de acceso de la suscripción; los role assignments sí.
- **`Key Vault Secrets User`** y no `Contributor` — leer el valor de un secreto es todo lo que
  ninguno de los dos principals necesita.
- **`enablePurgeProtection: true`** — impide que un borrado, accidental o malicioso, sea
  definitivo. Es irreversible a propósito.
- **`softDeleteRetentionInDays`: 90 en producción, 7 en el resto** — retención proporcional al
  costo de perder el secreto.
- **Diagnósticos a Log Analytics** — sin `AuditEvent` no hay forma de detectar el uso de una
  credencial filtrada.
- **Ningún valor de secreto en el Bicep** — un secreto escrito en una plantilla es un secreto
  versionado en Git.

## Despliegue

```bash
az deployment group create --resource-group rg-pdp-dev --template-file infra/keyvault/main.bicep --parameters infra/keyvault/main.parameters.dev.json
```

Repetir con `prod`. Antes hay que completar en el archivo de parámetros el `object id` de
la identidad administrada del App Service y el del service principal de la service connection.

Carga de un valor, siempre fuera de Git y fuera del historial del shell:

```bash
az keyvault secret set --vault-name kv-pdp-dev --name pdp-datasource-password --file ./secreto.txt
```

## Hospedaje de SurrealDB

`PDP_DATASOURCE_URL` no apunta a un servicio gestionado de Azure: apunta a una VM Linux que corre
SurrealDB en Docker, aprovisionada manualmente (sin Bicep todavía). La decisión completa — por qué
una VM y no un servicio de contenedores gestionado, y las restricciones de cuota de la suscripción
que la motivaron — vive en
[ADR-015 del repositorio de arquitectura](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-015-surrealdb-azure-hosting.md).
`vm-pdp-surrealdb-shared` se llamaba así por atender DEV y QA a la vez; QA se retiró (DEV/QA se
unificaron en un solo ambiente no-prod), así que hoy solo tiene un consumidor — el nombre quedó,
el propósito cambió.

Resumen operativo:

| Instancia | Ambientes | Aislamiento |
|---|---|---|
| `vm-pdp-surrealdb-shared` | DEV | `namespace`/`database` propios (`pdp_dev` — ver `application-dev.properties`) |
| `vm-pdp-surrealdb-prod` | PROD | Instancia y credenciales propias |

`datasourceUrl` en cada `ci/variables/*.yml` es la IP pública de la VM correspondiente —
configuración operativa, no secreto, igual que el resto de esa tabla. El puerto 8000 solo acepta
tráfico desde las IPs de salida conocidas del App Service de ese ambiente (regla de NSG explícita),
no desde Internet completo.

Reaprovisionar una VM (recreación, cambio de tamaño) invalida su contraseña de SurrealDB: hay que
generar una nueva y actualizar `pdp-datasource-password` en el Key Vault del ambiente afectado antes
del siguiente despliegue.

## Reglas que no se negocian

- Ningún secreto en el repositorio, en el YAML ni en los archivos de configuración versionados.
- Ningún secreto en logs: la aplicación registra identificadores y resultados, nunca payloads.
- Un vault por ambiente: un secreto de DEV nunca abre nada en producción.
- Rotación sin desplegar código: el valor cambia en el vault, no en el `pom.xml`.
