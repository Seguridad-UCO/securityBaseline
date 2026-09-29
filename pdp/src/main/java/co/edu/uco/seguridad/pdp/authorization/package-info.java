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
 *
 * <p>HU-017: se agrega {@code "resources :: usecase"} — mismo patrón que HU-016, para que
 * {@code AdministerResourceRegistrationUseCase} delegue en {@code RegisterProtectedResourceUseCase}
 * tras el mismo validador. {@code resources :: rule/:: dto/:: model/:: exception} ya estaban
 * declarados, así que esto no dispara la trampa de "primer NamedInterface del módulo".
 *
 * <p>HU-018: se agrega {@code "assignments :: rule"} — para que
 * {@code AdministerAssignmentRevocationUseCase} resuelva la aplicación de una asignación vía
 * {@code AssignmentApplicationLookupValidator}, publicado por primera vez desde ese subpaquete de
 * {@code assignments} (su propio {@code package-info.java} documenta que es el primer
 * {@code @NamedInterface} de ese subpaquete). {@code assignments :: usecase/:: dto} ya estaban
 * declarados desde HU-015, así que esto no dispara la trampa de "primer NamedInterface del módulo".
 *
 * <p>HU-019: se agregan {@code "profiles :: usecase"} y {@code "profiles :: dto"} (respuesta) —
 * primer consumidor externo de esos dos subpaquetes de {@code profiles} (sus propios
 * {@code package-info.java} lo documentan). {@code "profiles :: rule"} ya estaba publicado desde
 * HU-011 (lo consume {@code assignments}), así que agregarlo aquí no dispara la trampa de "primer
 * NamedInterface del módulo" — solo declara un consumidor nuevo de una interfaz ya nombrada.
 *
 * <p>HU-018/019 (fase de implementación): se agregan {@code "assignments :: model"} y
 * {@code "profiles :: model"} — los interactores de administración de asignaciones/perfiles
 * construyen {@code AssignmentId}/{@code ProfileAssignmentId}/{@code ProfileName} directamente para
 * armar las consultas de sus validadores de lookup. Primer consumidor externo de ambos subpaquetes
 * (sus propios {@code package-info.java} lo documentan) — trampa de Modulith detectada compilando,
 * tal como advierte {@code 1-planificador.md}.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {
        "commons",
        "applications", "applications :: rule", "applications :: dto", "applications :: exception",
        "applications :: usecase", "applications :: model", "applications :: aggregate",
        "resources", "resources :: repository", "resources :: aggregate", "resources :: rule", "resources :: dto", "resources :: model", "resources :: exception",
        "resources :: usecase",
        "assignments :: usecase", "assignments :: dto", "assignments :: rule", "assignments :: model",
        "identity :: usecase", "identity :: dto",
        "identity :: rule",
        "roles", "roles :: repository", "roles :: aggregate", "roles :: exception", "roles :: rule", "roles :: usecase", "roles :: dto", "roles :: model",
        "profiles", "profiles :: repository", "profiles :: aggregate", "profiles :: exception", "profiles :: rule", "profiles :: usecase", "profiles :: dto", "profiles :: model"})
package co.edu.uco.seguridad.pdp.authorization;
