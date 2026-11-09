/**
 * DTOs de entrada al núcleo de {@code identity} que otros módulos consumen. Mismo nombre de
 * NamedInterface ("dto") que {@code identity/application/primaryport/response} — ambos subpaquetes
 * forman una sola interfaz nombrada lógica, igual que en {@code roles :: dto} / {@code applications :: dto}.
 *
 * <p>Corrige una omisión de la PR #49 (rama {@code feature/ajustes_integracion_completa}):
 * {@code InternalAccessDecisionInteractorImpl} (en {@code authorization}) ya construía
 * {@code ResolveExternalIdentityRequest} directamente, sin que este paquete estuviera publicado ni
 * {@code "identity :: dto"} estuviera en el {@code allowedDependencies} de {@code authorization} —
 * {@code ModulithStructureTests} lo detectó al fusionar con el backlog de HU-016.
 */
@org.springframework.modulith.NamedInterface("dto")
package co.edu.uco.seguridad.pdp.identity.application.primaryport.request;
