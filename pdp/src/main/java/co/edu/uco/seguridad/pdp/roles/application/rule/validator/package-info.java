/**
 * Validadores que {@code roles} publica a otros módulos. Declarado ya como interfaz nombrada
 * aunque hoy nadie lo consuma: HU-005 (asignaciones) lo necesitará, y añadir la primera
 * {@code @NamedInterface} a un módulo que ya tiene consumidores es la trampa documentada en
 * {@code 1-planificador.md}.
 */
@org.springframework.modulith.NamedInterface("rule")
package co.edu.uco.seguridad.pdp.roles.application.rule.validator;
