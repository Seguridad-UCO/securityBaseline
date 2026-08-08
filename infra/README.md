# Infraestructura: Azure Key Vault y manejo de secretos

## Estado actual, sin adornos

**Hoy la aplicación no tiene ningún secreto.** No hay base de datos, no hay proveedor de identidad
y no hay exportador de telemetría remoto: todos los adaptadores son dummies en memoria. La
auditoría del repositorio lo confirma:

```bash
git grep -nIE "(password|secret|api[_-]?key|token|credential)" -- . ':!docs' ':!*.md'
```

Las únicas coincidencias son la documentación del propio wrapper de Maven y comentarios que
explican esta política. No hay `.env`, ni `.pem`, ni `.jks`, ni credenciales en
`application.properties`.

Esa es la respuesta concreta a la pregunta de Farid: no es que los secretos estén mal guardados,
es que todavía no existen. Lo que sí existe ahora es el mecanismo para cuando aparezcan, de modo
que la primera credencial real no tenga que improvisar dónde vivir.

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
| Client secret de Keycloak/OIDC | **Key Vault** | Permite suplantar a la aplicación |
| Token del colector OTLP | **Key Vault** | Permite inyectar o leer telemetría |

## Secretos reservados en el vault

Estos son los nombres que el pipeline ya sabe leer. Se crean vacíos o no se crean hasta que el
subsistema correspondiente exista; el pipeline solo los pedirá cuando la etapa de despliegue
esté activa.

| Secreto | Consumidor previsto | Variable de entorno |
|---|---|---|
| `pdp-datasource-password` | Adaptador SurrealDB (E-2) | `PDP_DATASOURCE_PASSWORD` |
| `pdp-oidc-client-secret` | Integración con el IdP (E-3) | `PDP_OIDC_CLIENT_SECRET` |
| `pdp-otlp-token` | Exportación de trazas | `PDP_OTLP_TOKEN` |

## Cómo se consumen

```text
Azure Key Vault
      │  (RBAC: Key Vault Secrets User)
      ├── service principal del pipeline ──► tarea AzureKeyVault@2
      │                                          │ variables enmascaradas
      │                                          ▼
      │                                     AzureWebApp@1 ─► app settings
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

Repetir con `qa` y `prod`. Antes hay que completar en el archivo de parámetros el `object id` de
la identidad administrada del App Service y el del service principal de la service connection.

Carga de un valor, siempre fuera de Git y fuera del historial del shell:

```bash
az keyvault secret set --vault-name kv-pdp-dev --name pdp-datasource-password --file ./secreto.txt
```

## Reglas que no se negocian

- Ningún secreto en el repositorio, en el YAML ni en los archivos de configuración versionados.
- Ningún secreto en logs: la aplicación registra identificadores y resultados, nunca payloads.
- Un vault por ambiente: un secreto de DEV nunca abre nada en producción.
- Rotación sin desplegar código: el valor cambia en el vault, no en el `pom.xml`.
