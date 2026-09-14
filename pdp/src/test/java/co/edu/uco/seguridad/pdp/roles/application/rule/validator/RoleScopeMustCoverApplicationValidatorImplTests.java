package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleCoverageQuery;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl.RoleScopeMustCoverApplicationValidatorImpl;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import co.edu.uco.seguridad.pdp.roles.domain.exception.ApplicationOutsideRoleScopeException;
import co.edu.uco.seguridad.pdp.roles.domain.exception.RoleNotFoundException;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleMustExistForTenantRule;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

class RoleScopeMustCoverApplicationValidatorImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final RoleId ROLE_ID = new RoleId(UUID.randomUUID());
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-12T00:00:00Z");

    private static final RoleMustExistForTenantRule NEVER_LETS_MISSING_THROUGH =
            input -> { throw new RoleNotFoundException(input.roleId()); };
    private static final RoleMustExistForTenantRule ACCEPTS_EXISTENCE = input -> { };

    @Test
    void rejects_when_the_role_does_not_exist() {
        RoleScopeMustCoverApplicationValidatorImpl validator = new RoleScopeMustCoverApplicationValidatorImpl(
                repositoryReturning(Mono.empty()), NEVER_LETS_MISSING_THROUGH, coverage -> { });

        StepVerifier.create(validator.execute(query()))
                .expectError(RoleNotFoundException.class)
                .verify();
    }

    @Test
    void completes_for_a_global_role_regardless_of_the_application() {
        Role role = Role.define(ROLE_ID, new RoleName("Docente"), RoleScope.global(), REGISTERED_AT);
        RoleScopeMustCoverApplicationValidatorImpl validator = new RoleScopeMustCoverApplicationValidatorImpl(
                repositoryReturning(Mono.just(role)), ACCEPTS_EXISTENCE, coverage -> { });

        StepVerifier.create(validator.execute(query())).verifyComplete();
    }

    @Test
    void rejects_a_tenant_scoped_role_that_does_not_match() {
        Role role = Role.define(ROLE_ID, new RoleName("Docente"), RoleScope.ofTenant(new TenantId("otra-universidad")),
                REGISTERED_AT);
        RoleScopeMustCoverApplicationValidatorImpl validator = new RoleScopeMustCoverApplicationValidatorImpl(
                repositoryReturning(Mono.just(role)), ACCEPTS_EXISTENCE,
                coverage -> { throw new ApplicationOutsideRoleScopeException(APPLICATION); });

        StepVerifier.create(validator.execute(query()))
                .expectError(ApplicationOutsideRoleScopeException.class)
                .verify();
    }

    @Test
    void completes_for_an_application_scoped_role_that_matches() {
        Role role = Role.define(ROLE_ID, new RoleName("Docente"), RoleScope.ofApplication(TENANT, APPLICATION), REGISTERED_AT);
        RoleScopeMustCoverApplicationValidatorImpl validator = new RoleScopeMustCoverApplicationValidatorImpl(
                repositoryReturning(Mono.just(role)), ACCEPTS_EXISTENCE, coverage -> { });

        StepVerifier.create(validator.execute(query())).verifyComplete();
    }

    private static RoleCoverageQuery query() {
        return new RoleCoverageQuery(ROLE_ID, TENANT, APPLICATION);
    }

    private static RoleRepository repositoryReturning(Mono<Role> result) {
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
                return result;
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
