/**
 * HU-015: se agregan {@code "applications :: usecase"}, {@code "applications :: model"} y
 * {@code "roles :: usecase"} — {@code assignments} es el único módulo que ya dependía a la vez de
 * {@code applications} y de {@code roles}, así que es el dueño natural de la orquestación
 * "registrar aplicación + crear su rol ADMIN + asignárselo al registrador" (mismo criterio que
 * llevó la saga de HU-010 a vivir en {@code resources}, no en {@code applications}).
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {
        "commons",
        "identity :: rule", "identity :: exception",
        "applications :: rule", "applications :: dto", "applications :: exception",
        "applications :: usecase", "applications :: model",
        "roles :: rule", "roles :: dto", "roles :: exception", "roles :: usecase",
        "profiles :: rule", "profiles :: dto"})
package co.edu.uco.seguridad.pdp.assignments;
