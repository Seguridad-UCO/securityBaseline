/**
 * Primer consumidor externo de este subpaquete (HU-017): {@code authorization} necesita
 * {@code RegisterProtectedResourceUseCase} para delegar en él tras gatear con
 * {@code PrincipalMustBeApplicationAdministratorValidator} — mismo patrón que {@code roles :: usecase}
 * en HU-016. {@code resources} ya tiene otros NamedInterfaces con consumidores (rule, dto, model,
 * exception, desde HU-004/HU-010), así que esto no dispara la trampa de "primer NamedInterface del
 * módulo" — solo abre este subpaquete concreto.
 */
@org.springframework.modulith.NamedInterface("usecase")
package co.edu.uco.seguridad.pdp.resources.application.usecase;
