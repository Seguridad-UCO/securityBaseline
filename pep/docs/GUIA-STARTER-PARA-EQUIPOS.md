# Guía de implementación del starter PEP para equipos de aplicaciones

## Antes de instalar

Este starter es para Java 25, Spring Boot 4.1 y WebFlux. Registra un backend ante un PEP externo; no protege
rutas dentro del proceso. Antes de comenzar, acuerde con el equipo de seguridad:

1. application ID y environment definitivos;
2. audiencia JWT que se exigirá;
3. URL pública HTTPS del PEP y URL privada HTTPS del backend;
4. token opaco de alta, almacenado como secreto;
5. conectividad PEP → backend y una regla de red que impida acceso directo externo;
6. recurso y política PDP, o la expectativa explícita de 503 mientras el PDP se implementa.

No use el starter para reemplazar validación de entrada, autorización fina de negocio, controles de base de datos
ni segmentación de red.

## 1. Instalar la dependencia

Mientras el artefacto sea local, desde `securityBaseline` instálelo:

```sh
./mvnw -f pep/starter/pom.xml install
```

Después agregue la dependencia:

```xml
<dependency>
  <groupId>co.edu.uco</groupId>
  <artifactId>security-pep-integration-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

No agregue un controlador de alta, filtros manuales ni una segunda llamada al PEP: la auto-configuración lo hace
al arrancar.

## 2. Configurar de forma segura

Use una propiedad de secretos o variables del despliegue para el token:

```properties
security.enabled=true
security.pep.registration.enabled=true
security.pep.registration.pep-url=https://security.example.org
security.pep.registration.application-id=academic
security.pep.registration.environment=prod
security.pep.registration.backend-url=https://academic.internal
security.pep.registration.audience=academic-api
security.pep.registration.token=${PEP_REGISTRATION_TOKEN}
security.pep.registration.allow-insecure-http=false
```

`backend-url` debe ser solo origen. Son inválidos `https://academic.internal/api`, una URL con query, fragmento o
credenciales. El PEP asigna automáticamente `/apps/academic`; no incluir ese prefijo en `backend-url`.

Para una demostración local aislada puede permitir HTTP de forma explícita:

```properties
security.pep.registration.allow-insecure-http=true
```

No lleve esa excepción a producción.

## 3. Arrancar y comprobar

Al obtener 2xx del PEP, el log incluye una URL como:

```text
PEP integration active: publicBaseUrl=https://security.example.org/apps/academic
```

Con 4xx revise token, application ID, environment y URL; tras corregir se requiere reiniciar. Con red caída o
5xx, la aplicación seguirá iniciando y el starter reintentará automáticamente.

Los clientes deben llamar, por ejemplo:

```text
https://security.example.org/apps/academic/api/v1/courses
```

No publique el puerto, load balancer o DNS interno del backend. Acceso directo significa saltarse el PEP.

## 4. Pruebas de aceptación

Después de crear la ruta, token y política PDP:

1. JWT válido, audiencia correcta y política allow: debe responder la API.
2. JWT sin audiencia válida: debe responder 401 y el backend no debe recibir la solicitud.
3. Política deny: debe responder 403 y no debe haber reenvío.
4. PDP apagado, timeout o respuesta inválida: debe responder 503 y no debe haber reenvío.
5. Desde una red no PEP, el backend directo debe ser inaccesible.

La aplicación de referencia es el directorio hermano `../pep-webflux-sample`. Tras instalar el starter, ejecute:

```sh
./mvnw -f ../pep-webflux-sample/pom.xml test
```

Esa prueba verifica el consumo del JAR, el alta HTTP simulada y `security.enabled=false`. El flujo PEP/PDP
completo se valida con el simulador y las pruebas descritas en `pep/README.md`.

## 5. Desactivar de forma controlada

```properties
security.enabled=false
```

La clave correcta es `security.enabled`. Al desactivarla, no hay alta ni reintentos del starter. No abre ni
bloquea un endpoint local; mantener el backend privado sigue siendo obligatorio. Para reactivarlo, ponga
`security.enabled=true` y confirme también `security.pep.registration.enabled=true`.

## Lista de salida a producción

- [ ] Versión del starter fijada y obtenida de un repositorio Maven confiable.
- [ ] Token de alta en un gestor de secretos y plan de rotación acordado.
- [ ] HTTPS en PEP y backend; `allow-insecure-http=false`.
- [ ] PEP alcanza al backend y el backend no es público.
- [ ] Credencial PEP, application ID, environment y audience coinciden.
- [ ] JWT correcto probado con issuer, firma y audience.
- [ ] Recurso y política disponibles en PDP/OPA.
- [ ] Casos allow, deny, JWT inválido y PDP no disponible comprobados.
- [ ] Monitoreo de PEP/PDP y logs de correlación habilitados.
