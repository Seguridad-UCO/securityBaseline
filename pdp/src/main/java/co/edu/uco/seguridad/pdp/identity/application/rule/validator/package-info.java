/**
 * Validadores que {@code identity} publica a otros módulos. Primer {@code @NamedInterface} de
 * este módulo (HU-005) — antes de este historia nadie declaraba {@code "identity"} en su
 * {@code allowedDependencies}, así que no restringe a ningún consumidor existente.
 */
@org.springframework.modulith.NamedInterface("rule")
package co.edu.uco.seguridad.pdp.identity.application.rule.validator;
