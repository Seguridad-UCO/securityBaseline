package co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash;
import co.edu.uco.seguridad.pdp.commons.model.*;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * {@code findUniqueByName} es {@code default} para que un doble anónimo que no lo necesita no tenga
 * que implementarlo — mismo criterio que {@code ProfileRepository.findById}
 * ({@code ProfileRepositoryTests}). Nada más lo ejercita: sin esta prueba, el cuerpo del default
 * queda sin cubrir y el paquete cae a 0 %.
 */
class ApplicationRepositoryTests {

    @Test
    void the_default_reports_the_search_as_not_implemented_unless_overridden() {
        ApplicationRepository repository = new ApplicationRepository() {
            @Override
            public Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name) {
                return Mono.just(false);
            }

            @Override
            public Mono<Boolean> existsByTenantAndId(TenantId tenantId, ApplicationId applicationId) {
                return Mono.just(false);
            }

            @Override
            public Mono<TenantId> findTenantIdById(ApplicationId applicationId) {
                return Mono.empty();
            }

            @Override
            public Mono<ApplicationCredentialHash> findCredentialHashById(ApplicationId applicationId) {
                return Mono.empty();
            }

            @Override
            public Mono<Application> findByIdForTenant(TenantId tenantId, ApplicationId applicationId) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> updateCredentialHash(ApplicationId applicationId, ApplicationCredentialHash credentialHash) {
                return Mono.empty();
            }

            @Override
            public Mono<ResultPage<Application>> findBy(ApplicationCriteria criteria, PageWindow window) {
                return Mono.empty();
            }

            @Override
            public Mono<Application> save(Application application) {
                return Mono.just(application);
            }

            @Override
            public Mono<Void> deleteById(ApplicationId applicationId) {
                return Mono.empty();
            }
        };

        StepVerifier.create(repository.findUniqueByName(new ApplicationName("cualquier-nombre")))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }
}
