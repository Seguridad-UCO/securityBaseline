/**
 * Value objects del núcleo de {@code assignments} que otros módulos consumen (HU-018/019):
 * {@code AssignmentId}, {@code ProfileAssignmentId} — {@code authorization} los necesita para
 * construir las consultas de sus validadores de lookup ({@code AssignmentApplicationLookupValidator},
 * {@code ProfileAssignmentApplicationLookupValidator}) desde el interactor.
 */
@org.springframework.modulith.NamedInterface("model")
package co.edu.uco.seguridad.pdp.assignments.domain.model;
