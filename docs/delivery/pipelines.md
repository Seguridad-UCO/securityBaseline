# Pipelines, SonarQube y Quality Gate

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

| Rama | Build + pruebas + Sonar | Despliegue |
|---|---|---|
| `feature/*` | sí | ninguno |
| `develop` | sí | DEV |
| `qa` | sí | QA |
| `main` | sí | PRODUCCIÓN |
| Pull request a `develop`/`qa`/`main` | sí | ninguno |

Los stages de despliegue verifican dos condiciones: la rama exacta y que la ejecución no sea de un
pull request. La segunda no es redundante — sin ella, un PR hacia `main` desplegaría producción con
código que aún no ha sido aprobado.

## Etapa de construcción y análisis

Un solo job, deliberadamente. SonarQube necesita las clases compiladas, los reportes de surefire y
el XML de JaCoCo del **mismo** workspace; separarlos obligaría a publicar y volver a descargar todo
`target/` sin ganar nada.

Orden de ejecución:

1. `JavaToolInstaller` fija el JDK 25 y `Cache@2` restaura `~/.m2`.
2. `SonarCloudPrepare@4` inyecta organización y token desde la service connection de SonarCloud.
3. `Maven@4` ejecuta `verify`: compilación, pruebas, reporte JaCoCo y análisis Sonar en un solo
   reactor, de modo que la cobertura que evalúa el gate es la que produjo este build.
4. `SonarCloudPublish@4` publica el resultado en el resumen del build.
5. `PublishCodeCoverageResults@2` publica la cobertura.
6. Empaquetado y publicación del artefacto, **omitidos en pull requests**: un PR se verifica, no se
   despacha.

## Quality Gate

El gate rompe el build por `sonar.qualitygate.wait=true`, pasado como propiedad extra en el
`SonarCloudPrepare`. Sin esa propiedad el análisis se publica pero el pipeline sigue en verde, que
es la falla silenciosa clásica de esta integración.

Analiza bugs, vulnerabilidades, security hotspots, code smells, duplicación y cobertura. La
cobertura llega desde `target/site/jacoco/jacoco.xml`, declarado en el `pom.xml` mediante
`sonar.coverage.jacoco.xmlReportPaths`.

`sonar.coverage.exclusions` excluye `*Configuration.java`, `PdpApplication.java` y `package-info.java`:
son cableado de Spring, y cubrirlos infla el porcentaje sin probar ninguna regla.

## Cobertura en SonarQube Cloud

Sonar **no calcula** cobertura: importa el XML que genera JaCoCo en el pipeline
(`target/site/jacoco/jacoco.xml`). Si el tablero muestra
*“A few extra steps are needed for SonarQube Cloud to analyze your code coverage”*, casi siempre
falta una de estas tres cosas:

1. **Análisis automático desactivado.** En SonarQube Cloud → proyecto → *Administration* →
   *Analysis Method*: usa solo CI-based analysis. El análisis automático (GitHub/ADO sin build)
   no lleva reporte JaCoCo y deja el tile de Coverage vacío.
2. **Pipeline que genera e importa el XML.** `mvn verify` debe producir
   `target/site/jacoco/jacoco.xml` **antes** de `sonar:sonar`. El `pom.xml` ya declara
   `sonar.coverage.jacoco.xmlReportPaths` y el pipeline comprueba que el archivo exista.
3. **Logs del análisis.** En el job de Azure DevOps, busca
   `Sensor JaCoCo XML Report Importer` y confirma que encontró el XML (no “No report imported”).
   El aviso amarillo *Last analysis had a warning* en el tablero suele detallar la misma causa.

Comprobación local (sin publicar a Sonar):

```bash
./mvnw verify
# debe existir:
# target/site/jacoco/jacoco.xml
```

## Configuración requerida en Azure DevOps

Nada de esto vive en el repositorio, y esa es la razón por la que hay que crearlo a mano una vez:

| Elemento | Nombre esperado | Contiene |
|---|---|---|
| Service connection **SonarCloud** | `SonarCloud-seguridad` (ver `ci/variables/common.yml`) | Token de análisis de sonarcloud.io |
| Service connection Azure | `Azure-PDP-Dev` / `-Qa` / `-Prod` | Credenciales de la suscripción |
| Environment | `pdp-dev`, `pdp-qa`, `pdp-prod` | Aprobaciones y checks |
| Extensión | **SonarQube Cloud** (`SonarSource.sonarcloud`) | Tareas `SonarCloudPrepare@4` / `SonarCloudPublish@4` |

Importante: la extensión *SonarQube Server* (`SonarQubePrepare@7`) y la de *SonarQube Cloud*
(`SonarCloudPrepare@4`) son distintas. Contra `sonarcloud.io` hay que usar la de Cloud; si no,
el scanner suele fallar con `Not authorized or project not found` al pedir feature flags.

## GitHub y Azure DevOps

```text
Desarrollador
      │ push a feature/*
      ▼
GitHub ──► webhook ──► Azure Pipelines
      │                      │ build · test · JaCoCo · SonarQube
      │                      ▼
      │                 Quality Gate
      │                      │
      ◄──── status check ────┘
      │
      ▼
Pull request bloqueado hasta que el check esté en verde
      │ merge a develop / qa / main
      ▼
Azure Pipelines ──► Key Vault ──► App Service (DEV / QA / PROD)
```

El repositorio vive en GitHub (`Seguridad-UCO/securityBaseline`) y el pipeline en Azure DevOps. La
conexión se establece con una service connection de GitHub; el resultado del pipeline vuelve al PR
como status check y se marca requerido en la protección de rama.

## Despliegue

Cada stage de despliegue usa un job `deployment` contra un `environment`, que es lo que habilita
aprobaciones, historial por ambiente y trazabilidad de qué versión está desplegada.

Pasos: descargar artefacto → leer secretos de Key Vault (`AzureKeyVault@2`) → desplegar en App
Service inyectando los secretos como *app settings* → verificar `/actuator/health` con reintentos.
El health check falla el despliegue si la aplicación no responde saludable, para que una versión
rota se vea aquí y no en la siguiente petición de un usuario.
