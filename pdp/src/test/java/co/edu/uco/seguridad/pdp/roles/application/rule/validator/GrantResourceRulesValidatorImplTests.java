package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.resources.domain.exception.ProtectedResourceNotFoundException;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.GrantResourceRequest;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl.GrantResourceRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import co.edu.uco.seguridad.pdp.roles.domain.exception.ResourceOutsideRoleScopeException;
import co.edu.uco.seguridad.pdp.roles.domain.exception.RoleNotFoundException;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

class GrantResourceRulesValidatorImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final RoleId ROLE_ID = new RoleId(UUID.randomUUID());
    private static final ResourceId RESOURCE = new ResourceId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final Role ROLE = Role.define(ROLE_ID, new RoleName("Docente"), RoleScope.ofTenant(TENANT),
            Instant.parse("2026-09-11T00:00:00Z"));

    private static final ProtectedResourceOwnerLookupValidator NEVER_CALLED_RESOURCE_OWNER = resourceId -> {
        throw new AssertionError("must not look up the resource when the role does not exist");
    };
    private static final ApplicationOwnerLookupValidator NEVER_CALLED_APPLICATION_OWNER = applicationId -> {
        throw new AssertionError("must not resolve the application tenant when the resource does not exist");
    };

    @Test
    void rejects_when_the_role_does_not_exist_for_the_tenant() {
        GrantResourceRulesValidatorImpl validator = new GrantResourceRulesValidatorImpl(
                repositoryReturning(Mono.empty()), roleMustExistRejecting(),
                NEVER_CALLED_RESOURCE_OWNER, NEVER_CALLED_APPLICATION_OWNER, coverage -> {
        });

        StepVerifier.create(validator.execute(request()))
                .expectError(RoleNotFoundException.class)
                .verify();
    }

    @Test
    void rejects_when_the_resource_does_not_exist() {
        GrantResourceRulesValidatorImpl validator = new GrantResourceRulesValidatorImpl(
                repositoryReturning(Mono.just(ROLE)), roleMustExistAccepting(),
                resourceId -> Mono.error(new ProtectedResourceNotFoundException(RESOURCE)),
                NEVER_CALLED_APPLICATION_OWNER, coverage -> {
        });

        StepVerifier.create(validator.execute(request()))
                .expectError(ProtectedResourceNotFoundException.class)
                .verify();
    }

    @Test
    void rejects_a_resource_outside_the_role_scope() {
        GrantResourceRulesValidatorImpl validator = new GrantResourceRulesValidatorImpl(
                repositoryReturning(Mono.just(ROLE)), roleMustExistAccepting(),
                resourceId -> Mono.just(APPLICATION), applicationId -> Mono.just(new TenantId("otra-universidad")),
                coverage -> {
                    throw new ResourceOutsideRoleScopeException(RESOURCE);
                });

        StepVerifier.create(validator.execute(request()))
                .expectError(ResourceOutsideRoleScopeException.class)
                .verify();
    }

    @Test
    void returns_the_role_when_every_rule_passes() {
        GrantResourceRulesValidatorImpl validator = new GrantResourceRulesValidatorImpl(
                repositoryReturning(Mono.just(ROLE)), roleMustExistAccepting(),
                resourceId -> Mono.just(APPLICATION), applicationId -> Mono.just(TENANT), coverage -> {
        });

        StepVerifier.create(validator.execute(request())).expectNext(ROLE).verifyComplete();
    }

    private static GrantResourceRequest request() {
        return new GrantResourceRequest(TENANT, ROLE_ID, RESOURCE);
    }

    private static co.edu.uco.seguridad.pdp.roles.domain.rule.RoleMustExistForTenantRule roleMustExistRejecting() {
        return input -> {
            throw new RoleNotFoundException(input.roleId());
        };
    }

    private static co.edu.uco.seguridad.pdp.roles.domain.rule.RoleMustExistForTenantRule roleMustExistAccepting() {
        return input -> {
        };
    }

    private static RoleRepository repositoryReturning(Mono<Role> role) {
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
                return role;
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
