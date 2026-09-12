package co.edu.uco.seguridad.pdp.roles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.DefineRoleRulesValidator;
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

/** Con las reglas sustituidas por un dummy que siempre aprueba: aquí se prueba la construcción y la persistencia, no las reglas. */
class DefineRoleUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final Instant NOW = Instant.parse("2026-09-11T00:00:00Z");

    @Test
    void defines_the_role_with_a_generated_id_and_the_current_time_and_no_resources() {
        List<Role> saved = new ArrayList<>();
        UUID fixedId = UUID.randomUUID();
        DefineRoleUseCaseImpl useCase = new DefineRoleUseCaseImpl(
                dto -> Mono.empty(), repositoryCapturing(saved), () -> fixedId, () -> NOW);

        DefineRoleRequest request = new DefineRoleRequest(new RoleName("Docente"), RoleScope.ofTenant(TENANT));

        StepVerifier.create(useCase.execute(request))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(new RoleId(fixedId));
                    assertThat(response.name()).isEqualTo(new RoleName("Docente"));
                    assertThat(response.scope()).isEqualTo(RoleScope.ofTenant(TENANT));
                    assertThat(response.resources()).isEmpty();
                    assertThat(response.registeredAt()).isEqualTo(NOW);
                })
                .verifyComplete();

        assertThat(saved).hasSize(1);
    }

    @Test
    void never_saves_when_the_rules_validator_rejects_the_request() {
        RuntimeException rejection = new RuntimeException("nombre repetido");
        DefineRoleRulesValidator alwaysRejects = dto -> Mono.error(rejection);
        DefineRoleUseCaseImpl useCase = new DefineRoleUseCaseImpl(
                alwaysRejects, unreachableRepository(), UUID::randomUUID, () -> NOW);

        DefineRoleRequest request = new DefineRoleRequest(new RoleName("Docente"), RoleScope.ofTenant(TENANT));

        StepVerifier.create(useCase.execute(request)).expectErrorMessage("nombre repetido").verify();
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
