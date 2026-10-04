# Acciones manuales de integración

El baseline local se instala automáticamente. Cada equipo debe:

1. Elegir el identificador de su aplicación, ambiente y rutas protegidas.
2. Registrar aplicación, recursos, roles/asignaciones y política OPA con el equipo de seguridad. Sin política, el PDP deniega el acceso.
3. Configurar su aplicación con la URL del PEP y la credencial que entregue el PDP. Nunca guardar secretos en el repositorio.
4. Hacer que el backend sea accesible únicamente mediante el PEP en ambientes compartidos.
5. Para Spring Boot WebFlux, consumir `co.edu.uco:security-pep-integration-spring-boot-starter:<versión>` desde GitHub Packages. GitHub Packages requiere un token personal con permiso `read:packages`, configurado localmente en `~/.m2/settings.xml` bajo el servidor `github`; ese token nunca se guarda en el repositorio.
6. En producción usar el despliegue corporativo: IdP real, TLS/mTLS, secretos administrados y observabilidad corporativa.
