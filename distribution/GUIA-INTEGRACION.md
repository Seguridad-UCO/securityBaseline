# Guía de integración local — Security Baseline

Esta guía es para los equipos que van a proteger una aplicación con Security Baseline en su computador. Explica el flujo completo: levantar el stack publicado, administrar la seguridad, descargar el starter Java y conectar una aplicación.

> Alcance: laboratorio local con HTTP y Docker Desktop. No requiere certificados, configurar Keycloak a mano ni crear claims. Para ambientes compartidos o producción se deben usar TLS, secretos administrados e identidad corporativa.

## 1. Qué instala esta distribución

Al levantar el `compose.yaml` se descargan estas imágenes publicadas en Docker Hub:

| Componente | Imagen | Función |
| --- | --- | --- |
| Keycloak local | `davidalzate/security-keycloak-local:<VERSION>` | Inicio de sesión e identidad. Importa el realm automáticamente. |
| PDP / BFF | `davidalzate/security-pdp:<VERSION>` | Administración de aplicaciones, recursos, roles, perfiles y decisiones. Conserva la sesión web. |
| PEP | `davidalzate/security-pep:<VERSION>` | Punto de aplicación de políticas al que consulta el starter de la aplicación. |
| OPA | `davidalzate/security-opa:<VERSION>` | Motor de políticas usado internamente por el PDP. |

También se crean contenedores locales de SurrealDB y Redis. Estos almacenan los datos del laboratorio y la sesión; no son servicios que el equipo deba configurar manualmente.

La aplicación de notas usada durante la validación **no forma parte de la distribución**. Es solo una prueba interna del equipo de Security Baseline.

## 2. Requisitos

- Docker Desktop iniciado, con Docker Compose v2 disponible (`docker compose version`).
- Puertos locales disponibles: `19090`, `18080` y `18081`.
- Para integrar el starter: Java 25, Spring Boot 4.1 y una aplicación WebFlux.
- Una cuenta de GitHub con permiso de lectura sobre `Seguridad-UCO/securityBaseline`, para descargar el paquete Maven.

No se necesita cuenta ni inicio de sesión de Docker Hub para descargar las imágenes públicas.

## 3. Descargar y arrancar el stack

Elige una versión publicada concreta. No uses `latest`: todos los componentes deben utilizar el mismo tag.

### macOS o Linux

```sh
mkdir security-baseline-local
cd security-baseline-local

# Sustituya <VERSION> por la versión publicada, por ejemplo 0.1.6.
curl -LO https://github.com/Seguridad-UCO/securityBaseline/releases/download/v<VERSION>/compose.yaml
curl -Lo .env https://github.com/Seguridad-UCO/securityBaseline/releases/download/v<VERSION>/.env.example
```

### Windows PowerShell

```powershell
mkdir security-baseline-local
cd security-baseline-local

# Sustituya <VERSION> por la versión publicada, por ejemplo 0.1.6.
Invoke-WebRequest https://github.com/Seguridad-UCO/securityBaseline/releases/download/v<VERSION>/compose.yaml -OutFile compose.yaml
Invoke-WebRequest https://github.com/Seguridad-UCO/securityBaseline/releases/download/v<VERSION>/.env.example -OutFile .env
```

Abra el archivo `.env` que acaba de descargar y deje, como mínimo, esta configuración:

```dotenv
SECURITY_DOCKER_ORG=davidalzate
SECURITY_VERSION=<VERSION>
KEYCLOAK_PORT=19090
PDP_PORT=18080
PEP_PORT=18081

# Solo para laboratorio local: no exige OTP ni WebAuthn al administrar seguridad.
PDP_SECURITY_MFA_ENABLED=false
```

El archivo `.env` se crea manualmente por cada equipo y es local: no se debe subir al repositorio de su aplicación si contiene valores propios del entorno.

Arranque y compruebe el estado:

```sh
docker compose up -d
docker compose ps
curl -fsS http://localhost:18080/actuator/health
curl -fsS http://localhost:18081/actuator/health
```

La primera descarga puede tardar varios minutos. Espere a que Keycloak y el PDP aparezcan saludables antes de intentar iniciar sesión.

Los datos locales se guardan en volúmenes Docker: `keycloak-data` conserva usuarios y contraseñas de Keycloak; `surrealdb-data` conserva aplicaciones, recursos, roles, perfiles y asignaciones; `redis-data` conserva caché y sesiones. Por tanto, detener o recrear contenedores no elimina la configuración.

### Puertos reservados por el baseline

| Servicio | URL local | Uso |
| --- | --- | --- |
| Keycloak | `http://keycloak.localhost:19090` | Login, registro local y administración técnica. |
| PDP / BFF | `http://localhost:18080` | API de administración, login y sesión HTTP. |
| PEP | `http://localhost:18081` | Decisiones solicitadas por las aplicaciones protegidas. |

Las aplicaciones consumidoras pueden conservar `8080`, `8081`, `5173`, `5174` u otros puertos: esos puertos no son usados por el stack publicado. Si alguno de los tres puertos reservados está ocupado, cambie únicamente el valor correspondiente en `.env` antes de ejecutar `docker compose up -d`.

## 4. Qué queda configurado automáticamente

Al primer arranque, la imagen de Keycloak importa el realm `security-baseline` con:

- clientes BFF y técnico del PEP;
- issuer, audiencias y JWKS que usan PDP y PEP;
- claim `tenant` y el claim `sub` generado desde el ID interno de Keycloak;
- login, registro local y sesión BFF;
- MFA deshabilitado para este laboratorio.

No edite claims, clientes, secretos internos ni redirect URIs en la consola de Keycloak. La imagen los entrega coherentes entre sí. Para administración técnica local, Keycloak está en `http://keycloak.localhost:19090` con `admin` / `admin`; esas credenciales son exclusivamente de laboratorio.

El usuario de demostración disponible es `security-demo` / `security-demo`. También se puede registrar un usuario local desde el flujo de inicio de sesión.

## 5. Abrir la administración de seguridad

El PDP es el BFF de administración. El frontend de administración debe consumir `http://localhost:18080` y usar sus cookies de sesión; los tokens nunca se guardan en el navegador.

Para integrar el microfrontend de administración en un frontend propio, configure como mínimo la URL del BFF y el nombre de la aplicación actual. Un ejemplo de variables de entorno de Vite es:

```dotenv
VITE_SECURITY_BFF_URL=http://localhost:18080
VITE_SECURITY_APPLICATION_NAME=mi-aplicacion
```

El artefacto o URL concreta del microfrontend se entrega por el canal de frontend de Security Baseline. No forma parte de las cuatro imágenes Docker y no es necesario para que el starter proteja el backend. Sí es el medio recomendado para que un administrador cree y mantenga el catálogo de seguridad.

## 6. Configuración manual obligatoria por aplicación

El arranque del baseline es automático, pero la seguridad de cada aplicación no puede adivinarse. El administrador de esa aplicación debe configurar, desde la administración de seguridad, lo siguiente:

1. **Registrar la aplicación.** Defina un nombre estable y único, por ejemplo `academico` o `notas`. Ese mismo nombre se usará en `security.pep.enforcement.application-name`.
2. **Guardar la credencial de integración.** Al crear la aplicación, guarde el secreto mostrado en el gestor de secretos del equipo. Nunca lo agregue al código, a un `.properties` versionado ni a capturas de pantalla.
3. **Registrar cada recurso HTTP protegido.** Un recurso está formado por método y ruta exacta; por ejemplo `GET /api/notas` y `POST /api/notas` son dos recursos distintos.
4. **Crear roles y asociar recursos a cada rol.** Crear un rol o agregarlo a un perfil no basta: el rol debe mostrar los recursos autorizados. Si no hay recurso asociado, el PDP responde `403 NO_APPLICABLE_POLICY`.
5. **Crear perfiles y añadirles los roles.** Por ejemplo, el perfil `Profesor` puede contener los roles de lectura y escritura.
6. **Asignar el perfil al usuario correcto.** La asignación se realiza para la aplicación actual. Verifique en “Personas y asignaciones” que el usuario y perfil aparezcan vinculados.
7. **Probar cada método HTTP.** Una operación con sesión válida pero sin recurso/rol/perfil aplicable responde `403`; no es un problema de login.

### Rotar una credencial de aplicación

Desde la ficha de la aplicación, use **Rotar credencial** cuando el secreto se haya expuesto, expire por política interna o cambie de responsable. La rotación invalida la credencial anterior: copie el nuevo valor en el secreto de despliegue de la aplicación y reiníciela. No es necesario recrear recursos, roles, perfiles ni asignaciones.

## 7. Descargar el starter Java desde GitHub Packages

El starter publicado es:

```text
co.edu.uco:security-pep-integration-spring-boot-starter:<STARTER_VERSION>
```

Las imágenes Docker y el starter tienen ciclos de publicación independientes:

- el tag `v<BASELINE_VERSION>` publica las cuatro imágenes del stack y los archivos de distribución;
- el tag `library-v<STARTER_VERSION>` publica el starter en GitHub Packages.

No es necesario publicar una nueva librería si solo cambió una imagen. El equipo integrador debe usar una versión del starter que haya sido probada con la versión del baseline que descarga.

GitHub Packages para Maven requiere autenticación incluso cuando el paquete sea visible públicamente. GitHub documenta el uso de un **Personal Access Token (classic)** con el alcance `read:packages` y acceso de lectura al repositorio o paquete. [Documentación oficial de GitHub](https://docs.github.com/es/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry?apiVersion=2022-11-28).

### 7.1 Crear el token de GitHub

En GitHub:

1. Abra su avatar → **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)**.
2. Seleccione **Generate new token (classic)**.
3. Marque únicamente `read:packages` para descargar la librería.
4. Genere el token, cópielo una sola vez y guárdelo en un gestor de secretos personal.

No comparta ni suba ese token al repositorio. Si se filtra, revóquelo en GitHub y cree uno nuevo.

### 7.2 Configurar Maven una vez por computador

Edite o cree:

- macOS/Linux: `~/.m2/settings.xml`
- Windows: `%USERPROFILE%\\.m2\\settings.xml`

Use su usuario de GitHub y el token. Se recomienda referenciar variables de entorno para no dejar el token escrito en el archivo:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>${env.GITHUB_PACKAGES_USER}</username>
      <password>${env.GITHUB_PACKAGES_TOKEN}</password>
    </server>
  </servers>
</settings>
```

Antes de ejecutar Maven, defina esas variables en su sistema o IDE:

```text
GITHUB_PACKAGES_USER=<su-usuario-de-github>
GITHUB_PACKAGES_TOKEN=<su-token-classic-con-read-packages>
```

En IntelliJ IDEA se pueden declarar en **Run → Edit Configurations → Environment variables**. En otros IDEs use el mecanismo equivalente. Esta configuración es por computador y funciona igual en macOS, Linux y Windows; no hay que añadir comandos específicos al código de la aplicación.

En el `pom.xml` de la aplicación que integra, agregue el repositorio y la dependencia:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/seguridad-uco/securitybaseline</url>
    <releases><enabled>true</enabled></releases>
    <snapshots><enabled>false</enabled></snapshots>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>co.edu.uco</groupId>
    <artifactId>security-pep-integration-spring-boot-starter</artifactId>
    <version><STARTER_VERSION></version>
  </dependency>
</dependencies>
```

Ejecute la compilación normal de la aplicación. Si Maven responde `401`, confirme que el token sea **classic**, tenga `read:packages`, no haya expirado y que el `id` del repositorio (`github`) coincida con el `id` definido en `settings.xml`.

## 8. Configurar el backend WebFlux

El starter actual es compatible con Spring Boot WebFlux. Protege todas las rutas por defecto y consulta al PEP antes de que la solicitud llegue al controlador.

Configure las propiedades en el mecanismo habitual de su aplicación. Para laboratorio local:

```properties
security.enabled=true
security.pep.enforcement.enabled=true
security.pep.enforcement.pep-url=http://localhost:18081
security.pep.enforcement.application-name=mi-aplicacion
security.pep.enforcement.environment=local
security.pep.enforcement.application-credential=${PEP_REGISTRATION_TOKEN}
security.pep.enforcement.allow-insecure-http=true

# Declare explícitamente las únicas rutas que no deben requerir autorización.
security.pep.enforcement.public-paths=/actuator/health
```

Las variables de entorno equivalentes son:

```text
SECURITY_ENABLED=true
SECURITY_PEP_ENFORCEMENT_ENABLED=true
SECURITY_PEP_ENFORCEMENT_PEP_URL=http://localhost:18081
SECURITY_PEP_ENFORCEMENT_APPLICATION_NAME=mi-aplicacion
SECURITY_PEP_ENFORCEMENT_ENVIRONMENT=local
SECURITY_PEP_ENFORCEMENT_APPLICATION_CREDENTIAL=<credencial-de-la-aplicacion>
SECURITY_PEP_ENFORCEMENT_ALLOW_INSECURE_HTTP=true
SECURITY_PEP_ENFORCEMENT_PUBLIC_PATHS=/actuator/health
```

`PEP_REGISTRATION_TOKEN` es la credencial emitida al registrar o rotar la aplicación en el PDP. No es el token de GitHub ni un access token de Keycloak. En local puede colocarla en una variable del IDE o un archivo ignorado por Git; en un despliegue real debe ir en el gestor de secretos de la plataforma.

No habilite `security.pep.registration.*` para esta integración normal. El starter embebido consulta al PEP publicado y este valida la credencial directamente contra el PDP.

## 9. Autenticación desde el frontend de la aplicación

Para que el starter pueda autorizar una llamada que viene de navegador, la solicitud al backend debe llevar una de estas dos evidencias:

- `Authorization: Bearer <access-token>`; o
- la cookie HttpOnly `SECURITY_BASELINE_SESSION` emitida por el BFF después del login.

En el laboratorio, el flujo recomendado es la cookie BFF:

1. El frontend dirige al usuario a iniciar sesión mediante el BFF `http://localhost:18080`.
2. Keycloak autentica al usuario y el BFF crea `SECURITY_BASELINE_SESSION`.
3. El frontend llama al backend con credenciales incluidas.
4. El starter recibe la cookie, el PEP recupera internamente el token del usuario y el PDP evalúa sus permisos.

Ejemplo de `fetch`:

```ts
await fetch("http://localhost:8082/api/notas", {
  credentials: "include",
});
```

El backend debe permitir el origen de su frontend y credenciales en CORS. Para un frontend Vite en `http://localhost:5174`, permita exactamente ese origen y `allowCredentials=true`; no use `*` cuando se envían cookies.

## 10. Interpretar respuestas y resolver problemas

| Respuesta | Significado | Qué revisar |
| --- | --- | --- |
| `401 TOKEN_INVALID` | Falta sesión/Bearer o el token no es válido. | Inicie sesión otra vez, confirme que la cookie se envía o que el Bearer corresponde al realm local. |
| `403 ACCESS_DENIED` | La identidad es válida, pero no hay política aplicable. | Revise método/ruta exactos, recurso asociado al rol, rol en el perfil y perfil asignado al usuario. |
| `403 NO_APPLICABLE_POLICY` en auditoría | No existe una relación aplicable entre recurso, rol y usuario. | Asociar el recurso al rol: el perfil por sí solo no concede acceso. |
| `503 PEP_UNAVAILABLE` | El backend no logra llegar al PEP/PDP. | Confirme `http://localhost:18081/actuator/health`, la URL del PEP y que Docker esté activo. |
| Maven `401` | GitHub Packages rechazó las credenciales. | Revise `settings.xml`, el token classic, `read:packages` y acceso al repositorio. |
| Puerto ocupado | Otro proceso usa un puerto del baseline. | Cambie el puerto correspondiente en `.env`, ejecute `docker compose up -d` y actualice las URLs consumidoras. |

Para ver contenedores y logs:

```sh
docker compose ps
docker compose logs -f pdp pep keycloak
```

## 11. Actualizar a una nueva versión

1. Detenga la aplicación consumidora si hace falta reiniciarla.
2. Edite `SECURITY_VERSION` en `.env` y coloque el nuevo tag publicado.
3. Descargue las nuevas imágenes y recree el stack:

```sh
docker compose pull
docker compose up -d
docker compose ps
```

Un `pull`, `up -d` o `down` normal **no elimina** los datos: Keycloak, SurrealDB y Redis usan volúmenes Docker. Para reiniciar el laboratorio desde cero, incluido usuarios, aplicaciones, roles y credenciales, el comando destructivo es:

```sh
docker compose down -v
```

Úselo solamente si desea borrar deliberadamente todo el laboratorio.

Cuando una versión incluye cambios del realm de Keycloak, el cambio automático aplica a instalaciones nuevas. Para una instalación local ya existente, siga las notas de migración de esa release o recree el laboratorio solo si no necesita conservar sus datos.

> Instalaciones creadas con una distribución anterior que no tuviera el volumen `keycloak-data` no pueden recuperar usuarios que ya se hayan perdido al eliminar el contenedor. Registre de nuevo el usuario una única vez —preferiblemente con el mismo correo— y compruebe/asigne su perfil. Desde la versión que incorpora esta corrección, los usuarios permanecen tras `down` y `up -d`.

## 12. Límites del laboratorio y paso a producción

Esta distribución está diseñada para integración local sencilla:

- usa HTTP y cookies no seguras deliberadamente;
- el administrador local de Keycloak y el usuario demo no son credenciales de producción;
- MFA está deshabilitado por defecto para que no bloquee las pruebas;
- la observabilidad corporativa no se incluye para evitar conflictos de puertos y porque cada equipo puede tener su propia plataforma de monitoreo.

Para producción se requiere, como mínimo: HTTPS, identidad corporativa, MFA para administradores, secretos fuera del repositorio, CORS/reverse proxy revisados, PEP/PDP en red privada, observabilidad corporativa y una política de actualización coordinada.

## 13. Lista final de verificación

- [ ] Docker Desktop está activo y el stack responde saludable.
- [ ] El equipo usa el mismo `<VERSION>` en las cuatro imágenes.
- [ ] La aplicación está registrada y su credencial está guardada fuera del repositorio.
- [ ] Cada método/ruta protegida fue registrado como recurso.
- [ ] Cada rol tiene sus recursos asociados.
- [ ] Cada perfil tiene sus roles asociados.
- [ ] El usuario de prueba tiene el perfil correcto en la aplicación correcta.
- [ ] Maven descarga el starter `<STARTER_VERSION>` desde GitHub Packages usando un token classic con `read:packages`.
- [ ] El backend tiene el starter y configura `application-name`, PEP URL, ambiente y credencial.
- [ ] El frontend envía cookie de sesión o Bearer y CORS permite credenciales.
- [ ] Se validaron al menos una operación permitida y una denegada.
