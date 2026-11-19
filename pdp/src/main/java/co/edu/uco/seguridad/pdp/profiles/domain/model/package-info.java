/**
 * Value objects del núcleo de {@code profiles} que otros módulos consumen (HU-019):
 * {@code ProfileName} — {@code authorization} lo necesita para construir {@code DefineProfileRequest}
 * en {@code AdministerProfileDefinitionInteractorImpl}.
 */
@org.springframework.modulith.NamedInterface("model")
package co.edu.uco.seguridad.pdp.profiles.domain.model;
