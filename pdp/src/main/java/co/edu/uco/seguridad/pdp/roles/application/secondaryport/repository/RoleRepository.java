package co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import reactor.core.publisher.Mono;

/** Puerto secundario del catálogo de roles, expresado solo en tipos de dominio. */
public interface RoleRepository {

    /** Unicidad dentro del alcance exacto: nivel + inquilino + aplicación. */
    Mono<Boolean> existsByNameInScope(RoleName name, RoleScope scope);

    /** Vacío si el rol no existe o no es de ese inquilino (un rol global tampoco lo es). */
    Mono<Role> findByIdForTenant(RoleId roleId, TenantId tenantId);

    /** Vacío si el rol no existe. A diferencia de {@link #findByIdForTenant}, no filtra por inquilino: un rol GLOBAL debe encontrarse igual (HU-005). */
    Mono<Role> findById(RoleId roleId);

    /** Los roles del inquilino más los globales, recortados en la propia consulta. */
    Mono<ResultPage<Role>> findBy(RoleCriteria criteria, PageWindow window);

    Mono<Role> save(Role role);
}
