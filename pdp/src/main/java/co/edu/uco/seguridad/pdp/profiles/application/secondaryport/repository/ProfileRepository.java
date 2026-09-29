package co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.domain.Profile;
import co.edu.uco.seguridad.pdp.profiles.domain.ProfileCriteria;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import reactor.core.publisher.Mono;

/** Puerto secundario del catálogo de perfiles, expresado solo en tipos de dominio. */
public interface ProfileRepository {

    /** Unicidad dentro del alcance exacto: nivel + inquilino + aplicación. */
    Mono<Boolean> existsByNameInScope(ProfileName name, RoleScope scope);

    /** Vacío si el perfil no existe o no es de ese inquilino (un perfil global tampoco lo es). */
    Mono<Profile> findByIdForTenant(ProfileId profileId, TenantId tenantId);

    /** Lookup sin filtro de tenant para enriquecer hechos ya autorizados por assignments. */
    default Mono<Profile> findById(ProfileId profileId) { return Mono.empty(); }

    Mono<ResultPage<Profile>> findBy(ProfileCriteria criteria, PageWindow window);

    Mono<Profile> save(Profile profile);

    default Mono<Void> deleteById(ProfileId profileId) { return Mono.error(new UnsupportedOperationException()); }

    /** True si algún perfil aún contiene el rol; protege su borrado. */
    default Mono<Boolean> existsByRoleId(RoleId roleId) { return Mono.just(false); }

    /** True si existen perfiles cuyo alcance pertenece a la aplicación. */
    default Mono<Boolean> existsByApplicationId(ApplicationId applicationId) { return Mono.just(false); }
}
