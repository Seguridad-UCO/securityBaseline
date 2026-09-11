# ADR: PEP proxy independiente en el repositorio de la plataforma

Estado: implementado para contrato v1; integración con PDP/OPA real pendiente.
Fecha: 2026-09-06. Alcance aprobado: proxy preparado para SDK posterior, Bearer primero.

## Contexto confirmado

El repositorio de arquitectura fija proxy para el MVP, Clean Architecture hexagonal, WebFlux y Modulith
en cada contenedor Java. El C4 L3 del PEP distingue ingreso, normalización, cliente PDP y enforcement.
La demo usa MVC/HttpClient bloqueante y su interceptor siempre permite continuar: es evidencia conceptual.

La guía docs/07-engineering/repository-structure.md del repositorio de arquitectura sigue como stub;
base_architecture aporta primitivas, no un proyecto PEP completo. El mapa de módulos y el código PDP
actual son la referencia concreta. El PDP ya tiene identidad local, tenants y BFF OIDC, pero carece de
EvaluarAcceso y cliente OPA. ADR-022 y algunos diagramas de baseline todavía describen estados anteriores.

## Decisiones

1. Mantener el PDP en la raíz y crear pep/ con POM autónomo, mismo runtime, artefacto y escaneo propios.
   No convertir la raíz en agregador Maven ni mover src/ del PDP. Contrato HTTP versionado compartido
   como OpenAPI/JSON Schema, sin depender de clases del otro sistema.
2. Paquetes co.edu.uco.seguridad.pep: application, commons, ingress, normalization y enforcement.
   El cliente HTTP del PDP es un adaptador secundario de enforcement: el puerto `DecisionPort` es
   propiedad del caso de uso y no existe un módulo cliente que invierta esa dependencia.
3. Dominio y commons son Java puro; aplicación permite Reactor para sus puertos de I/O; WebFlux,
   serialización, tokens y clientes remotos se resuelven en adaptadores. Interactores HTTP permanecen
   en infraestructura, siguiendo el PDP actual y evitando DTO web en aplicación.
4. Casos de uso, reglas y validadores tienen interfaz e implementación. Las operaciones reactivas usan
   un único DTO de entrada y extienden contratos reactivos compartidos; `DecisionPort` pertenece a
   enforcement y se implementa por el adaptador HTTP del PDP. No inventar repositorios o reglas de
   catálogo dentro del PEP.
5. Los controladores son adaptadores primarios: delegan a interactores o casos de uso expuestos. El
   flujo protegido es captura → normalización → enforcement → proxy; el proxy se suscribe únicamente
   después de un ALLOW verificado.
6. PEP aplica decisiones, PDP construye contexto confiable y OPA evalúa reglas Rego. Denegación técnica
   por evidencia inválida o resultado no aplicable no representa una política de negocio en Java.
7. Bearer JWT RS256 con JWKS, issuer, vigencia, sub y audiencia por ruta. Tenant/roles/perfiles locales
   se resuelven en el PDP; no duplicar su administración ni asumir que tenant está siempre en el JWT.
8. El nuevo endpoint interno propuesto es POST /internal/v1/access-decisions, con mTLS del servicio
   y Bearer del usuario. Está implementado como cliente PEP y simulador externo de test; el PDP real
   debe incorporarlo con seguridad independiente de sus sesiones de navegador.
9. No caché de decisiones, reintentos de operaciones, fail-open ni ALLOW simulado en el artefacto productivo.
   Obligaciones no soportadas y respuestas ambiguas fallan cerradas.
10. Puertos locales PDP 8080 y PEP 8081 preservan consumidores actuales. Son distintos de los números
    ilustrativos del deployment documental y se mantienen configurables.

## Dependencias permitidas

| Módulo | Dependencias |
|---|---|
| application | contratos reactivos compartidos |
| commons | Java |
| normalization | commons, application |
| enforcement | commons, application |
| ingress | commons, application, DTOs/casos de uso expuestos de normalización y enforcement |

Modulith verifica los límites. ArchUnit comprueba que aplicación no importa infraestructura/Spring,
dominio/commons no importan Reactor/Spring/aplicación, y PEP no importa PDP/shared ni Servlet.

## Límites de confianza

Aplicaciones destino privadas y seleccionadas por configuración; ninguna URL del usuario puede actuar
como destino. Ruta validada una sola vez para autorización y forwarding. Credenciales no se propagan
al backend por defecto. Headers entrantes tienen una lista explícita permitida; no se aceptan identidades
arbitrarias por header. Las respuestas con JSON duplicado, IDs incorrectos o enum desconocido se rechazan.

Query y cuerpo no son atributos de política v1. Las operaciones cuya autorización dependa de esos datos
necesitan ampliar el contexto del PDP antes de integrarse. Roles/perfiles serán datos de entrada de OPA.

## Validación y pendientes

Las verificaciones automáticas cubren HTTP real, JWT, contrato observado contra schemas, mTLS,
aislamiento de módulos, errores, manipulación de rutas/headers, concurrencia, límites y cancelación.
Fixtures solo existen en src/test y en el target Docker explícito fixtures.

Quedan fuera de esta entrega: implementar la operación PDP/OPA/auditoría, SDK, login del PEP,
cuotas distribuidas y SLOs de carga. La prueba de rendimiento es funcional y de concurrencia acotada;
no certifica throughput en producción. Docker requiere un daemon operativo. El pipeline separado
ci/pep-pipeline.yml debe registrarse en Azure para ejecutarse remotamente; no tiene despliegue automático.

No se modificó la fuente original de arquitectura ni la configuración de autenticación del PDP.
