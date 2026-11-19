package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleOwnershipQuery;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl.RoleMustExistForTenantValidatorImpl;
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
import java.util.UUID;

class RoleMustExistForTenantValidatorImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());
    private static final Role FOUND = Role.define(ROLE, new RoleName("Docente"), RoleScope.ofTenant(TENANT),
            Instant.parse("2026-09-12T00:00:00Z"));

    @Test
    void completes_when_the_role_exists_for_the_tenant() {
        RoleMustExistForTenantValidatorImpl validator = new RoleMustExistForTenantValidatorImpl(
                repositoryFinding(FOUND), new RoleMustExistForTenantRuleImpl());

        StepVerifier.create(validator.execute(new RoleOwnershipQuery(ROLE, TENANT))).verifyComplete();
    }

    @Test
    void refuses_a_role_that_does_not_exist_for_the_tenant() {
        RoleMustExistForTenantValidatorImpl validator = new RoleMustExistForTenantValidatorImpl(
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
