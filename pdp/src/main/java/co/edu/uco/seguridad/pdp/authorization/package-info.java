/**
 * HU-015: se agrega {@code "applications :: usecase"} para que el nuevo
 * {@code AdministerApplicationRemovalUseCase}/{@code AdministerApplicationCredentialRotationUseCase}
 * puedan delegar en {@code RemoveApplicationUseCase}/{@code RotateApplicationCredentialUseCase} tras
 * validar con {@code PrincipalMustBeApplicationAdministratorValidator} — ya lo consume
 * {@code resources}, así que no es el primer consumidor de ese NamedInterface.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {
        "commons",
        "applications", "applications :: rule", "applications :: dto", "applications :: exception",
        "applications :: usecase", "applications :: model",
        "resources", "resources :: rule", "resources :: dto", "resources :: model", "resources :: exception",
        "assignments :: usecase", "assignments :: dto",
        "roles :: rule"})
package co.edu.uco.seguridad.pdp.authorization;
