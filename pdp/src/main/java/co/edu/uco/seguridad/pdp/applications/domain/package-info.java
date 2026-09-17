/**
 * Agregado de {@code applications} publicado para que otros módulos lo consuman (PR #55,
 * {@code EvaluateInternalAccessUseCaseImpl}): resuelve la aplicación completa por nombre vía
 * {@code ApplicationNameLookupValidator} y necesita {@code Application.tenantId()}/{@code id()}
 * directamente. Primer consumidor externo del agregado — hasta ahora {@code domain.model},
 * {@code domain.exception} y las capas de {@code application} ya eran interfaz nombrada, pero la
 * raíz de {@code domain} (donde vive {@code Application.java}) no lo era: trampa de Modulith
 * detectada al compilar, tal como advierte {@code 1-planificador.md}.
 */
@org.springframework.modulith.NamedInterface("aggregate")
package co.edu.uco.seguridad.pdp.applications.domain;
