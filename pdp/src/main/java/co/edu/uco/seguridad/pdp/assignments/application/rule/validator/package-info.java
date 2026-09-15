/**
 * Validadores que {@code assignments} publica a otros módulos (HU-018): {@code authorization}
 * necesita {@code AssignmentApplicationLookupValidator} para gatear la revocación de una asignación
 * sin conocer {@code AssignmentRepository} directamente.
 */
@org.springframework.modulith.NamedInterface("rule")
package co.edu.uco.seguridad.pdp.assignments.application.rule.validator;
