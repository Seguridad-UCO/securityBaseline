package co.edu.uco.seguridad.pdp.roles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.GrantResourceRequest;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.GrantResourceRulesValidator;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El validador ya devolvió el rol correcto y validado (R3-R5) — este caso de uso solo lo transforma
 * (Role.withResource) y lo guarda. No vuelve a consultar ni a decidir nada.
 */
class GrantResourceToRoleUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final RoleId ROLE_ID = new RoleId(UUID.randomUUID());
    private static final ResourceId RESOURCE = new ResourceId(UUID.randomUUID());
    private static final Role ROLE = Role.define(ROLE_ID, new RoleName("Docente"), RoleScope.ofTenant(TENANT),
            Instant.parse("2026-09-11T00:00:00Z"));

    @Test
    void saves_the_role_with_the_resource_added_and_returns_it() {
        List<Role> saved = new ArrayList<>();
        GrantResourceToRoleUseCaseImpl useCase = new GrantResourceToRoleUseCaseImpl(
                request -> Mono.just(ROLE), repositoryCapturing(saved));

        StepVerifier.create(useCase.execute(new GrantResourceRequest(TENANT, ROLE_ID, RESOURCE)))
                .assertNext(response -> assertThat(response.resources()).containsExactly(RESOURCE))
                .verifyComplete();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).resources()).containsExactly(RESOURCE);
    }

    @Test
    void never_saves_when_the_rules_validator_rejects_the_request() {
        RuntimeException rejection = new RuntimeException("recurso fuera de alcance");
        GrantResourceRulesValidator alwaysRejects = request -> Mono.error(rejection);
        GrantResourceToRoleUseCaseImpl useCase = new GrantResourceToRoleUseCaseImpl(alwaysRejects, unreachableRepository());

        StepVerifier.create(useCase.execute(new GrantResourceRequest(TENANT, ROLE_ID, RESOURCE)))
                .expectErrorMessage("recurso fuera de alcance")
                .verify();
    }

    private static RoleRepository repositoryCapturing(List<Role> saved) {
        return new RoleRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(RoleName name, RoleScope scope) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Role> findByIdForTenant(RoleId roleId, TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResultPage<Role>> findBy(RoleCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Role> save(Role role) {
                saved.add(role);
                return Mono.just(role);
            }
        };
    }

    private static RoleRepository unreachableRepository() {
        return new RoleRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(RoleName name, RoleScope scope) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Role> findByIdForTenant(RoleId roleId, TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResultPage<Role>> findBy(RoleCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Role> save(Role role) {
                throw new AssertionError("must not save when the rules reject the request");
            }
        };
    }
}
