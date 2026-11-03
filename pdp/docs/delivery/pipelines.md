# Pipelines, SonarQube Cloud y Quality Gate

[← Estrategia de ramas](branching-strategy.md) · [Secretos →](../../infra/README.md)

## Estructura

```text
azure-pipelines.yml              punto de entrada: triggers y composición de stages
ci/
├── variables/
│   ├── common.yml               ajustes compartidos, ningún secreto
│   ├── dev.yml  qa.yml  prod.yml   un archivo por ambiente
└── templates/
    ├── steps-java-setup.yml     JDK + caché de Maven
    ├── stage-build-analyze.yml  compilar, probar, analizar, empaquetar
    └── stage-deploy.yml         despliegue parametrizado, instanciado 3 veces
```

El pipeline es uno solo, no uno por ambiente. La verificación debe ser idéntica en las tres ramas y
duplicar el YAML es exactamente cómo se consigue que dejen de serlo. Lo único que varía por
ambiente es el archivo de variables y el `environment` de Azure DevOps.

## Triggers

| Rama                                 | Build + pruebas + Sonar | Despliegue |
|--------------------------------------|-------------------------|------------|
| `feature/*`                          | sí                      | ninguno    |
| `develop`                            | sí                      | DEV        |
| `qa`                                 | sí                      | QA         |
| `main`                               | sí                      | PRODUCCIÓN |
| Pull request a `develop`/`qa`/`main` | sí                      | ninguno    |

Los stages de despliegue verifican dos condiciones: la rama exacta y que la ejecución no sea de un
pull request. La segunda no es redundante — sin ella, un PR hacia `main` desplegaría producción con
código que aún no ha sido aprobado.

## Etapa de construcción y análisis

Un solo job, deliberadamente. SonarQube Cloud necesita las clases compiladas, los reportes de
surefire y el XML de JaCoCo del **mismo** workspace; separarlos obligaría a publicar y volver a
descargar todo `target/` sin ganar nada.

Orden de ejecución:

1. `checkout: self` con `fetchDepth: 0` — Sonar necesita historial completo (blame, *new code*) y,
   en pull requests, la referencia de la rama base, que un clon superficial no trae.
2. `JavaToolInstaller` fija el JDK y `Cache@2` restaura `~/.m2`.
3. Un paso de shell resuelve los parámetros de Sonar según el tipo de build: en un pull request
   calcula `sonarPrKey`/`sonarPrBranch`/`sonarPrBase` y trae la rama base con `git fetch`; en un
   build de rama normal solo confirma el nombre de la rama. Nunca se fija `sonar.branch.name` en
   un build de pull request — son modos mutuamente excluyentes en el análisis de Sonar.
4. `SonarCloudPrepare@4` inyecta organización y token desde la service connection de SonarCloud —
   una de dos variantes según sea build de rama o de pull request, cada una con las propiedades que
   ese modo requiere.
5. `Maven@4` ejecuta `verify`: compilación, pruebas, reporte JaCoCo y análisis Sonar en un solo
   reactor, de modo que la cobertura que evalúa el gate es la que produjo este build.
   `codeCoverageToolOption: 'None'` es deliberado — JaCoCo ya vive en `pom.xml`; dejar que la tarea
   de Maven también lo inyecte duplica el plugin y puede hacer fallar el parser de cobertura de
   Azure después de un `verify` exitoso.
6. Un paso de shell confirma que `target/site/jacoco/jacoco.xml` existe antes de publicar — sin este
   archivo, SonarQube Cloud no tiene cobertura que mostrar y falla en silencio.
7. `SonarCloudPublish@4` publica el resultado en el resumen del build.
8. `PublishCodeCoverageResults@2` publica la cobertura en la pestaña de Azure DevOps.
9. Empaquetado (`./mvnw package -DskipTests`) y publicación del artefacto JAR, **omitidos en pull
   requests**: un PR se verifica, no se despacha. Este JAR es un artefacto de evidencia/depuración —
   el despliegue real no lo usa (ver [Despliegue](#despliegue)).

## Quality Gate

El gate rompe el build por `sonar.qualitygate.wait=true`, pasado como propiedad extra en
`SonarCloudPrepare`. Sin esa propiedad el análisis se publica pero el pipeline sigue en verde, que
es la falla silenciosa clásica de esta integración.

Analiza bugs, vulnerabilidades, security hotspots, code smells, duplicación y cobertura. La
cobertura llega desde `target/site/jacoco/jacoco.xml`, declarado en el `pom.xml` mediante
`sonar.coverage.jacoco.xmlReportPaths` — esa ruta vive **solo** en `pom.xml`, como ruta absoluta vía
`${project.build.directory}`. No se repite en el pipeline: una copia relativa ahí terminó
resolviéndose contra el directorio de trabajo del proceso de análisis (que no es necesariamente el
raíz del repositorio) y dejaba a Sonar sin encontrar el reporte pese a que el archivo sí existía.

`sonar.coverage.exclusions` excluye `*Configuration.java`, `PdpApplication.java` y `package-info.java`:
son cableado de Spring, y cubrirlos infla el porcentaje sin probar ninguna regla.

`sonar.issue.ignore.multicriteria` silencia `java:S110` (profundidad de jerarquía de herencia) en
todo el proyecto. El plan gratuito de SonarQube Cloud no permite Quality Gates ni Quality Profiles
personalizados por proyecto, así que ignorar la regla desde las propiedades de análisis es la única
palanca disponible sin actualizar de plan.

## Cobertura en SonarQube Cloud

Sonar **no calcula** cobertura: importa el XML que genera JaCoCo en el pipeline. Si el tablero
muestra *"A few extra steps are needed for SonarQube Cloud to analyze your code coverage"*, casi
siempre falta una de estas cosas:

1. **Análisis automático desactivado.** En SonarQube Cloud → proyecto → *Administration* →
   *Analysis Method*: usa solo CI-based analysis. El análisis automático (GitHub/ADO sin build)
   no lleva reporte JaCoCo y deja el tile de Coverage vacío.
2. **Pipeline que genera e importa el XML antes de analizar.** `mvn verify` debe producir
   `target/site/jacoco/jacoco.xml` **antes** de `sonar:sonar`; el orden de goals en un mismo
   reactor de Maven ya garantiza esto.
3. **Logs del análisis.** En el job de Azure DevOps, busca `Sensor JaCoCo XML Report Importer` y
   confirma que encontró el XML (no "No report imported").

Comprobación local (sin publicar a Sonar):

```bash
./mvnw verify
# debe existir:
# target/site/jacoco/jacoco.xml
```

## Configuración requerida en Azure DevOps

Nada de esto vive en el repositorio, y esa es la razón por la que hay que crearlo a mano una vez:

| Elemento                          | Nombre esperado                                        | Contiene                                                                                                                                                                                                                             |
|-----------------------------------|--------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Service connection **SonarCloud** | `SonarCloud-seguridad` (ver `ci/variables/common.yml`) | Token de análisis de sonarcloud.io                                                                                                                                                                                                   |
| Service connection Azure          | `Azure-PDP-Dev` / `-Qa` / `-Prod`                      | Credenciales de la suscripción, con rol Contributor a nivel de suscripción (necesario para `az acr build` sobre un registro que vive en el resource group de DEV, y para desplegar en resource groups distintos del propio ambiente) |
| Environment                       | `pdp-dev`, `pdp-qa`, `pdp-prod`                        | Aprobaciones y checks                                                                                                                                                                                                                |
| Extensión                         | **SonarQube Cloud** (`SonarSource.sonarcloud`)         | Tareas `SonarCloudPrepare@4` / `SonarCloudPublish@4`                                                                                                                                                                                 |

Importante: la extensión *SonarQube Server* (`SonarQubePrepare@7`) y la de *SonarQube Cloud*
(`SonarCloudPrepare@4`) son distintas. Contra `sonarcloud.io` hay que usar la de Cloud; si no,
el scanner suele fallar con `Not authorized or project not found` al pedir feature flags.

## GitHub y Azure DevOps

```text
Desarrollador
      │ push a feature/*
      ▼
GitHub ──► webhook ──► Azure Pipelines
      │                      │ build · test · JaCoCo · SonarQube Cloud
      │                      ▼
      │                 Quality Gate
      │                      │
      ◄──── status check ────┘
      │
      ▼
Pull request bloqueado hasta que el check esté en verde
      │ merge a develop / qa / main
      ▼
Azure Pipelines ──► Key Vault ──► Azure Container Registry ──► App Service (DEV / QA / PROD)
```

El repositorio vive en GitHub (`Seguridad-UCO/securityBaseline`) y el pipeline en Azure DevOps. La
conexión se establece con una service connection de GitHub; el resultado del pipeline vuelve al PR
como status check y se marca requerido en la protección de rama.

## Despliegue

Cada stage de despliegue usa un job `deployment` contra un `environment`, que es lo que habilita
aprobaciones, historial por ambiente y trazabilidad de qué versión está desplegada.

El artefacto que se despliega es una **imagen de contenedor**, no el JAR que publica la etapa de
construcción: el runtime de Java gestionado por Azure App Service llega hasta Java 21, y este
proyecto compila contra Java 25 (`pom.xml`). Desplegar el JAR sobre ese runtime falla al cargar
clases; el contenedor (`Dockerfile`, `eclipse-temurin:25-jre`) es lo que mantiene consistente la
versión de Java entre build y ejecución.

Pasos:

1. Leer secretos de Key Vault (`AzureKeyVault@2`).
2. `az acr build` compila la imagen **dentro de** Azure Container Registry a partir del `Dockerfile`
   del repositorio — el agente no necesita Docker local. La etiqueta es el `Build.BuildId`, nunca
   `latest`: App Service no vuelve a consultar una etiqueta cuando su contenido cambia, así que una
   etiqueta fija dejaría corriendo la versión anterior hasta un reinicio manual.
3. `az webapp config container set` apunta el App Service a la imagen recién publicada, y
   `az webapp config appsettings set` inyecta los secretos como variables de entorno del proceso.
4. `az webapp restart` fuerza a que el contenedor nuevo arranque de inmediato.
5. Verificar `/actuator/health` con reintentos. El health check falla el despliegue si la
   aplicación no responde saludable, para que una versión rota se vea aquí y no en la siguiente
   petición de un usuario. Un 502/503 sostenido casi siempre significa que SurrealDB no es
   alcanzable en `PDP_DATASOURCE_URL` — ver [`infra/README.md`](../../infra/README.md).
