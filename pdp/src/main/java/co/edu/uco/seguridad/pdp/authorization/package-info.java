/**
 * HU-015: se agrega {@code "applications :: usecase"} para que el nuevo
 * {@code AdministerApplicationRemovalUseCase}/{@code AdministerApplicationCredentialRotationUseCase}
 * puedan delegar en {@code RemoveApplicationUseCase}/{@code RotateApplicationCredentialUseCase} tras
 * validar con {@code PrincipalMustBeApplicationAdministratorValidator} — ya lo consume
 * {@code resources}, así que no es el primer consumidor de ese NamedInterface.
 *
 * <p>HU-016: se agregan {@code "roles :: usecase"}, {@code "roles :: dto"} y {@code "roles :: model"}
 * — mismo patrón, para que {@code AdministerRoleDefinitionUseCase}/{@code AdministerResourceGrantUseCase}
 * deleguen en {@code DefineRoleUseCase}/{@code GrantResourceToRoleUseCase} tras el mismo validador, y
 * para que los mappers/interactores de este módulo lean {@code RoleScope}/{@code RoleName} (PLAN-HU-016.md
 * §8) al construir el {@code AdministrationRequest} y al aplanar la respuesta. {@code roles :: rule}
 * ya estaba declarado (lo consume {@code ActiveRoleNamesLookupValidator}), y los tres subpaquetes ya
 * son interfaz nombrada con consumidores en otros módulos desde HU-004/HU-011/HU-015, así que esto no
 * dispara la trampa de "primer NamedInterface del módulo".
 *
 * <p>{@code "identity :: dto"} corrige una omisión de la PR #49: {@code InternalAccessDecisionInteractorImpl}
 * ya construía {@code ResolveExternalIdentityRequest} sin que ese paquete estuviera publicado —
 * {@code ModulithStructureTests} lo marcó al fusionar con el backlog de HU-016.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {
        "commons",
        "applications", "applications :: rule", "applications :: dto", "applications :: exception",
        "applications :: usecase", "applications :: model",
        "resources", "resources :: rule", "resources :: dto", "resources :: model", "resources :: exception",
        "assignments :: usecase", "assignments :: dto",
        "identity :: usecase", "identity :: dto",
        "identity :: rule",
        "roles :: rule", "roles :: usecase", "roles :: dto", "roles :: model"})
package co.edu.uco.seguridad.pdp.authorization;
