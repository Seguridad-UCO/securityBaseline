package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Operación compensatoria (ver RegisterProtectedApplicationUseCaseImpl): delega directo al
 * repositorio, sin reglas propias. Lo único que vale la pena probar es que delega con el id
 * correcto y que completa incluso cuando el repositorio no tenía nada que borrar.
 */
class RemoveApplicationUseCaseImplTests {

    @Test
    void delegates_deletion_to_the_repository_with_the_given_id() {
        List<ApplicationId> deleted = new ArrayList<>();
        ApplicationRepository repository = new ApplicationRepository() {
            @Override
            public Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> existsByTenantAndId(TenantId tenantId, ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<TenantId> findTenantIdById(ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash> findCredentialHashById(
                    ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResultPage<Application>> findBy(ApplicationCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Application> save(Application application) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> deleteById(ApplicationId applicationId) {
                deleted.add(applicationId);
                return Mono.empty();
            }
        };
        ApplicationId id = new ApplicationId(UUID.randomUUID());

        StepVerifier.create(new RemoveApplicationUseCaseImpl(repository).execute(id)).verifyComplete();

        assertThat(deleted).containsExactly(id);
    }
}
