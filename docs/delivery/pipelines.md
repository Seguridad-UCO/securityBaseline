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
2. `SonarQubePrepare@7` inyecta URL y token desde la service connection.
3. `Maven@4` ejecuta `verify`: compilación, pruebas, reporte JaCoCo y análisis Sonar en un solo
   reactor, de modo que la cobertura que evalúa el gate es la que produjo este build.
4. `SonarQubePublish@7` publica el resultado en el resumen del build.
5. `PublishCodeCoverageResults@2` publica la cobertura.
6. Empaquetado y publicación del artefacto, **omitidos en pull requests**: un PR se verifica, no se
   despacha.

## Quality Gate

El gate rompe el build por `sonar.qualitygate.wait=true`, pasado como propiedad extra en el
`SonarQubePrepare`. Sin esa propiedad el análisis se publica pero el pipeline sigue en verde, que
es la falla silenciosa clásica de esta integración.

Analiza bugs, vulnerabilidades, security hotspots, code smells, duplicación y cobertura. La
cobertura llega desde `target/site/jacoco/jacoco.xml`, declarado en el `pom.xml` mediante
`sonar.coverage.jacoco.xmlReportPaths`.

`sonar.coverage.exclusions` excluye `*Configuration.java`, `PdpApplication.java` y `package-info.java`:
son cableado de Spring, y cubrirlos infla el porcentaje sin probar ninguna regla.

Cobertura actual de la línea base: **92,7 % de instrucciones, 91,5 % de líneas, 89,5 % de ramas**
sobre 109 pruebas.

## Configuración requerida en Azure DevOps

Nada de esto vive en el repositorio, y esa es la razón por la que hay que crearlo a mano una vez:

| Elemento | Nombre esperado | Contiene |
|---|---|---|
| Service connection SonarQube | `SonarQube-UCO` | URL del servidor y token de análisis |
| Service connection Azure | `Azure-PDP-Dev` / `-Qa` / `-Prod` | Credenciales de la suscripción |
| Environment | `pdp-dev`, `pdp-qa`, `pdp-prod` | Aprobaciones y checks |
| Extensión | SonarQube (SonarSource) | Tareas `SonarQubePrepare@7` / `SonarQubePublish@7` |

Si la organización tiene instalada una versión anterior de la extensión, las tareas son `@5` o
`@6`; el resto del YAML no cambia.

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
