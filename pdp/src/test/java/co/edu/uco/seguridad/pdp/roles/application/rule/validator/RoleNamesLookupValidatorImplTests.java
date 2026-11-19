package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl.RoleNamesLookupValidatorImpl;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Un id que ya no existe se omite del resultado: es enriquecimiento, no una regla que rechaza.
 */
class RoleNamesLookupValidatorImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-12T00:00:00Z");
    private static final RoleId COORDINATOR_ID = new RoleId(UUID.randomUUID());
    private static final RoleId MISSING_ID = new RoleId(UUID.randomUUID());
    private static final Role COORDINATOR = Role.define(COORDINATOR_ID,
            new RoleName("Coordinador académico"), RoleScope.ofTenant(TENANT), REGISTERED_AT);

    @Test
    void resolves_the_names_of_the_roles_that_exist() {
        RoleNamesLookupValidatorImpl validator = new RoleNamesLookupValidatorImpl(
                repositoryWith(Map.of(COORDINATOR_ID, COORDINATOR)));

        StepVerifier.create(validator.execute(Set.of(COORDINATOR_ID)))
                .assertNext(names -> org.assertj.core.api.Assertions.assertThat(names).containsExactly("Coordinador académico"))
                .verifyComplete();
    }

    @Test
    void omits_a_role_id_that_no_longer_exists_without_failing() {
        RoleNamesLookupValidatorImpl validator = new RoleNamesLookupValidatorImpl(
                repositoryWith(Map.of(COORDINATOR_ID, COORDINATOR)));

        StepVerifier.create(validator.execute(Set.of(COORDINATOR_ID, MISSING_ID)))
                .assertNext(names -> org.assertj.core.api.Assertions.assertThat(names).containsExactly("Coordinador académico"))
                .verifyComplete();
    }

    @Test
    void an_empty_set_of_ids_resolves_to_an_empty_set_of_names() {
        RoleNamesLookupValidatorImpl validator = new RoleNamesLookupValidatorImpl(repositoryWith(Map.of()));

        StepVerifier.create(validator.execute(Set.of()))
                .assertNext(names -> org.assertj.core.api.Assertions.assertThat(names).isEmpty())
                .verifyComplete();
    }

    private static RoleRepository repositoryWith(Map<RoleId, Role> roles) {
        return new RoleRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(RoleName name, RoleScope scope) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Role> findByNameInScope(RoleName name, RoleScope scope) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Role> findByIdForTenant(RoleId roleId, TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Role> findById(RoleId roleId) {
                return Mono.justOrEmpty(roles.get(roleId));
            }

            @Override
            public Mono<ResultPage<Role>> findBy(RoleCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Role> save(Role role) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
