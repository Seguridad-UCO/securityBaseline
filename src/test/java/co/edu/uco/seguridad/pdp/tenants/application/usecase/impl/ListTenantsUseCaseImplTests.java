package co.edu.uco.seguridad.pdp.tenants.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.secondaryport.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantName;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantStatus;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class ListTenantsUseCaseImplTests {

    private static final Tenant TENANT = Tenant.register(new TenantId("universidad-uco"), new TenantName("UCO"));

    @Test
    void lists_every_tenant_projected_to_the_response() {
        ListTenantsUseCaseImpl useCase = new ListTenantsUseCaseImpl(fakeRepository(Flux.just(TENANT)));

        StepVerifier.create(useCase.execute())
                .assertNext(responses -> {
                    assertThat(responses).hasSize(1);
                    assertThat(responses.getFirst().id()).isEqualTo(TENANT.id());
                    assertThat(responses.getFirst().name()).isEqualTo(TENANT.name());
                    assertThat(responses.getFirst().status()).isEqualTo(TenantStatus.ACTIVE);
                })
                .verifyComplete();
    }

    @Test
    void returns_an_empty_list_when_there_are_no_tenants() {
        ListTenantsUseCaseImpl useCase = new ListTenantsUseCaseImpl(fakeRepository(Flux.empty()));

        StepVerifier.create(useCase.execute())
                .assertNext(responses -> assertThat(responses).isEmpty())
                .verifyComplete();
    }

    private static TenantRepository fakeRepository(Flux<Tenant> tenants) {
        return new TenantRepository() {
            @Override
            public Mono<TenantStatus> findStatusById(TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> existsById(TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Tenant> save(Tenant tenant) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Flux<Tenant> findAll() {
                return tenants;
            }
        };
    }
}
