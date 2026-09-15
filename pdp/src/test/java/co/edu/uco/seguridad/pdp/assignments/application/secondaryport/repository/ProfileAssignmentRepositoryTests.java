package co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * {@code findActiveProfileIdsFor} es {@code default} para que un doble anónimo que no lo necesita
 * (como los de {@code AssignProfileUseCaseImplTests}) no tenga que implementarlo. Nada más lo
 * ejercita: sin esta prueba, el cuerpo del default queda sin cubrir y el paquete cae a 0 %.
 */
class ProfileAssignmentRepositoryTests {

    @Test
    void the_default_reports_no_active_profile_ids_unless_overridden() {
        ProfileAssignmentRepository repository = new ProfileAssignmentRepository() {
            @Override
            public Mono<Boolean> existsActiveByUserApplicationProfile(
                    UserId userId, ApplicationId applicationId, ProfileId profileId, Instant now) {
                return Mono.just(false);
            }

            @Override
            public Mono<ProfileAssignment> findByIdForTenant(
                    ProfileAssignmentId profileAssignmentId, TenantId tenantId) {
                return Mono.empty();
            }

            @Override
            public Mono<ResultPage<ProfileAssignment>> findBy(ProfileAssignmentCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ProfileAssignment> save(ProfileAssignment profileAssignment) {
                return Mono.just(profileAssignment);
            }
        };

        StepVerifier.create(repository.findActiveProfileIdsFor(
                        new UserId(UUID.randomUUID()), new ApplicationId(UUID.randomUUID()), Instant.now()))
                .expectNext(Set.of())
                .verifyComplete();
    }
}
