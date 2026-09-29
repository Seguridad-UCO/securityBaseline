package co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import reactor.core.publisher.Mono;

/**
 * Puerto secundario del catálogo de roles, expresado solo en tipos de dominio.
 */
public interface RoleRepository {

    /**
     * Unicidad dentro del alcance exacto: nivel + inquilino + aplicación.
     */
    Mono<Boolean> existsByNameInScope(RoleName name, RoleScope scope);

    /**
     * Resuelve el rol con ese nombre en ese alcance exacto, si existe (HU-015: backfill de
     * administrador — reutiliza un rol {@code ADMIN} creado a mano en vez de duplicarlo). Vacío si
     * no hay ninguno. Complemento de {@link #existsByNameInScope}, que solo responde si existe.
     */
    Mono<Role> findByNameInScope(RoleName name, RoleScope scope);

    /**
     * Vacío si el rol no existe o no es de ese inquilino (un rol global tampoco lo es).
     */
    Mono<Role> findByIdForTenant(RoleId roleId, TenantId tenantId);

    /**
     * Vacío si el rol no existe. A diferencia de {@link #findByIdForTenant}, no filtra por inquilino: un rol GLOBAL debe encontrarse igual (HU-005).
     */
    Mono<Role> findById(RoleId roleId);

    /**
     * Los roles del inquilino más los globales, recortados en la propia consulta.
     */
    Mono<ResultPage<Role>> findBy(RoleCriteria criteria, PageWindow window);
    default Mono<Long> countByApplication(TenantId tenantId, ApplicationId applicationId) { return Mono.error(new UnsupportedOperationException()); }

    Mono<Role> save(Role role);

    default Mono<Void> deleteById(RoleId roleId) {
        return Mono.error(new UnsupportedOperationException());
    }

    /**
     * True si algún rol aún conserva el grant; protege el borrado del recurso.
     */
    default Mono<Boolean> existsByResourceId(ResourceId resourceId) {
        return Mono.just(false);
    }

    /**
     * True si existen roles cuyo alcance pertenece a la aplicación.
     */
    default Mono<Boolean> existsByApplicationId(ApplicationId applicationId) {
        return Mono.just(false);
    }
}
