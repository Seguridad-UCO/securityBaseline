package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * DTO crudo: Strings desnudos, sin anotaciones. Misma forma que
 * {@code roles.infrastructure...raw.DefineRoleRawRequest} (HU-016): vive aquí porque el endpoint que
 * lo recibe se mueve a {@code authorization} (ver PLAN-HU-016.md §6) — no es un contrato nuevo, es
 * la misma forma en el módulo que ahora aloja el adaptador primario.
 */
public record DefineRoleRawRequest(String name, String scope, String applicationId) {
}
