package co.edu.uco.seguridad.pdp.tenants.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.exception.DuplicateTenantException;
import co.edu.uco.seguridad.pdp.tenants.application.primaryport.request.CreateTenantRequest;
import co.edu.uco.seguridad.pdp.tenants.application.secondaryport.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.application.rule.impl.TenantCodeMustBeUniqueRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantName;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantStatus;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateTenantUseCaseImplTests {

    private static final TenantId ID = new TenantId("universidad-uco");
    private static final TenantName NAME = new TenantName("Universidad UCO");

    @Test
    void creates_a_tenant_that_does_not_exist_yet() {
        List<Tenant> saved = new ArrayList<>();
        TenantRepository repository = fakeRepository(false, saved);
        CreateTenantUseCaseImpl useCase = new CreateTenantUseCaseImpl(
                new TenantCodeMustBeUniqueRuleImpl(repository), repository);

        StepVerifier.create(useCase.execute(new CreateTenantRequest(ID, NAME)))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(ID);
                    assertThat(response.name()).isEqualTo(NAME);
                    assertThat(response.status()).isEqualTo(TenantStatus.ACTIVE);
                })
                .verifyComplete();
        assertThat(saved).hasSize(1);
    }

    @Test
    void refuses_a_tenant_id_that_already_exists() {
        TenantRepository repository = fakeRepository(true, new ArrayList<>());
        CreateTenantUseCaseImpl useCase = new CreateTenantUseCaseImpl(
                new TenantCodeMustBeUniqueRuleImpl(repository), repository);

        StepVerifier.create(useCase.execute(new CreateTenantRequest(ID, NAME)))
                .expectError(DuplicateTenantException.class)
                .verify();
    }

    private static TenantRepository fakeRepository(boolean exists, List<Tenant> saved) {
        return new TenantRepository() {
            @Override
            public Mono<Tenant> findById(TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> existsById(TenantId tenantId) {
                return Mono.just(exists);
            }

            @Override
            public Mono<Tenant> save(Tenant tenant) {
                saved.add(tenant);
                return Mono.just(tenant);
            }

            @Override
            public Flux<Tenant> findAll() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
