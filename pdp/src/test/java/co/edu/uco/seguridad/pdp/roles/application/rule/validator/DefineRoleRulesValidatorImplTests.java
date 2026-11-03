package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl.DefineRoleRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import co.edu.uco.seguridad.pdp.roles.domain.exception.DuplicateRoleNameException;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleNameMustBeUniqueInScopeRule;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

/**
 * Orden documentado en el esqueleto: si el alcance trae aplicación, se verifica primero (R2) y
 * después se resuelve la disponibilidad del nombre (R1) — mismo patrón de "lo barato primero" que
 * RegisterApplicationRulesValidatorImpl.
 */
class DefineRoleRulesValidatorImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());

    private static final ApplicationMustExistForTenantValidator NEVER_CALLED_APPLICATION_VALIDATOR = query -> {
        throw new AssertionError("must not check application existence for a tenant-scoped role");
    };
    private static final RoleNameMustBeUniqueInScopeRule NEVER_LETS_A_NAME_THROUGH =
            input -> {
                throw new DuplicateRoleNameException(input.name(), input.scope());
            };
    private static final RoleNameMustBeUniqueInScopeRule ALWAYS_ACCEPTS_THE_NAME = input -> {
    };

    @Test
    void completes_for_a_tenant_scoped_role_with_a_free_name() {
        DefineRoleRulesValidatorImpl validator = new DefineRoleRulesValidatorImpl(
                NEVER_CALLED_APPLICATION_VALIDATOR, ALWAYS_ACCEPTS_THE_NAME, repositoryReporting(false));

        StepVerifier.create(validator.execute(request(RoleScope.ofTenant(TENANT)))).verifyComplete();
    }

    @Test
    void completes_for_an_application_scoped_role_when_the_application_exists_and_the_name_is_free() {
        DefineRoleRulesValidatorImpl validator = new DefineRoleRulesValidatorImpl(
                query -> Mono.empty(), ALWAYS_ACCEPTS_THE_NAME, repositoryReporting(false));

        StepVerifier.create(validator.execute(request(RoleScope.ofApplication(TENANT, APPLICATION))))
                .verifyComplete();
    }

    @Test
    void rejects_a_name_already_taken_in_the_scope() {
        DefineRoleRulesValidatorImpl validator = new DefineRoleRulesValidatorImpl(
                NEVER_CALLED_APPLICATION_VALIDATOR, NEVER_LETS_A_NAME_THROUGH, repositoryReporting(true));

        StepVerifier.create(validator.execute(request(RoleScope.ofTenant(TENANT))))
                .expectError(DuplicateRoleNameException.class)
                .verify();
    }

    @Test
    void rejects_an_application_scoped_role_when_the_application_does_not_exist() {
        DefineRoleRulesValidatorImpl validator = new DefineRoleRulesValidatorImpl(
                query -> Mono.error(new ApplicationNotFoundException(query.applicationId())),
                ALWAYS_ACCEPTS_THE_NAME, unreachableRepository());

        StepVerifier.create(validator.execute(request(RoleScope.ofApplication(TENANT, APPLICATION))))
                .expectError(ApplicationNotFoundException.class)
                .verify();
    }

    private static DefineRoleRequest request(RoleScope scope) {
        return new DefineRoleRequest(new RoleName("Docente"), scope);
    }

    private static RoleRepository repositoryReporting(boolean nameTaken) {
        return new RoleRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(RoleName name, RoleScope scope) {
                return Mono.just(nameTaken);
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

    private static RoleRepository unreachableRepository() {
        return new RoleRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(RoleName name, RoleScope scope) {
                throw new AssertionError("must not check name availability when the application does not exist");
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
