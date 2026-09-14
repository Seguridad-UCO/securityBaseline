package co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.domain.Profile;
import co.edu.uco.seguridad.pdp.profiles.domain.ProfileCriteria;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

/**
 * {@code findById} es {@code default} para que un doble anónimo que no lo necesita (como los de
 * {@code DefineProfileUseCaseImplTests}) no tenga que implementarlo. Nada más lo ejercita: sin esta
 * prueba, el cuerpo del default queda sin cubrir y el paquete cae a 0 %.
 */
class ProfileRepositoryTests {

    @Test
    void the_default_finds_no_profile_unless_overridden() {
        ProfileRepository repository = new ProfileRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(ProfileName name, RoleScope scope) {
                return Mono.just(false);
            }

            @Override
            public Mono<Profile> findByIdForTenant(ProfileId profileId, TenantId tenantId) {
                return Mono.empty();
            }

            @Override
            public Mono<ResultPage<Profile>> findBy(ProfileCriteria criteria, PageWindow window) {
                return Mono.empty();
            }

            @Override
            public Mono<Profile> save(Profile profile) {
                return Mono.just(profile);
            }
        };

        StepVerifier.create(repository.findById(new ProfileId(UUID.randomUUID()))).verifyComplete();
    }
}
