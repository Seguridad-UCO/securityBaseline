package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleOwnershipQuery;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl.RoleApplicationLookupValidatorImpl;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import co.edu.uco.seguridad.pdp.roles.domain.exception.RoleNotFoundException;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.rule.impl.RoleMustExistForTenantRuleImpl;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-016: resuelve a qué aplicación pertenece un rol, para gatear GrantResourceToRole.
 */
class RoleApplicationLookupValidatorImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-14T00:00:00Z");

    @Test
    void resolves_the_application_id_of_an_application_scoped_role() {
        Role role = Role.define(ROLE, new RoleName("Docente"), RoleScope.ofApplication(TENANT, APPLICATION), REGISTERED_AT);
        RoleApplicationLookupValidatorImpl validator = new RoleApplicationLookupValidatorImpl(
                repositoryFinding(role), new RoleMustExistForTenantRuleImpl());

        StepVerifier.create(validator.execute(new RoleOwnershipQuery(ROLE, TENANT)))
                .assertNext(result -> assertThat(result).contains(APPLICATION))
                .verifyComplete();
    }

    @Test
    void resolves_empty_for_a_tenant_scoped_role() {
        Role role = Role.define(ROLE, new RoleName("Coordinador"), RoleScope.ofTenant(TENANT), REGISTERED_AT);
        RoleApplicationLookupValidatorImpl validator = new RoleApplicationLookupValidatorImpl(
                repositoryFinding(role), new RoleMustExistForTenantRuleImpl());

        StepVerifier.create(validator.execute(new RoleOwnershipQuery(ROLE, TENANT)))
                .assertNext(result -> assertThat(result).isEqualTo(Optional.<ApplicationId>empty()))
                .verifyComplete();
    }

    @Test
    void refuses_a_role_that_does_not_exist_for_the_tenant() {
        RoleApplicationLookupValidatorImpl validator = new RoleApplicationLookupValidatorImpl(
                repositoryFinding(null), new RoleMustExistForTenantRuleImpl());

        StepVerifier.create(validator.execute(new RoleOwnershipQuery(ROLE, TENANT)))
                .expectError(RoleNotFoundException.class)
                .verify();
    }

    private static RoleRepository repositoryFinding(Role found) {
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
                return found == null ? Mono.empty() : Mono.just(found);
            }

            @Override
            public Mono<Role> findById(RoleId roleId) {
                throw new UnsupportedOperationException();
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
