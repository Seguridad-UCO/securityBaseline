package co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
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

    Mono<ResultPage<Profile>> findBy(ProfileCriteria criteria, PageWindow window);

    Mono<Profile> save(Profile profile);
}
