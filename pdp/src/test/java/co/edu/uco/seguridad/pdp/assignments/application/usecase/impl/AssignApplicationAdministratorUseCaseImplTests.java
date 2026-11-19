package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignApplicationAdministratorRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignRoleUseCase;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleLookupByNameInScopeValidator;
import co.edu.uco.seguridad.pdp.roles.application.usecase.DefineRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Backfill manual (HU-015): encuentra o crea el rol ADMIN de la aplicación y lo asigna. No decide
 * negocio propio — el "encuentra o crea" no es una regla, es una consulta con `if (...) return;`
 * (sb-arquitectura, FASE 4 del plan).
 */
class AssignApplicationAdministratorUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION_ID = new ApplicationId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final RoleScope ADMIN_SCOPE = RoleScope.ofApplication(TENANT, APPLICATION_ID);
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-13T00:00:00Z");
    private static final AssignApplicationAdministratorRequest REQUEST =
            new AssignApplicationAdministratorRequest(TENANT, APPLICATION_ID, USER);

    @Test
    void defines_the_admin_role_when_it_does_not_exist_yet_and_assigns_it() {
        List<DefineRoleRequest> defined = new ArrayList<>();
        List<AssignRoleRequest> assigned = new ArrayList<>();
        RoleId createdRoleId = new RoleId(UUID.randomUUID());
        AssignApplicationAdministratorUseCaseImpl useCase = new AssignApplicationAdministratorUseCaseImpl(
                roleLookupResolving(null), defineRoleSucceeding(defined, createdRoleId), assignRoleSucceeding(assigned));

        StepVerifier.create(useCase.execute(REQUEST)).assertNext(response -> {
            assertThat(response.userId()).isEqualTo(USER);
            assertThat(response.roleId()).isEqualTo(createdRoleId);
        }).verifyComplete();

        assertThat(defined).hasSize(1);
        assertThat(defined.getFirst().name()).isEqualTo(new RoleName("ADMIN"));
        assertThat(defined.getFirst().scope()).isEqualTo(ADMIN_SCOPE);
        assertThat(assigned).hasSize(1);
        assertThat(assigned.getFirst().roleId()).isEqualTo(createdRoleId);
        assertThat(assigned.getFirst().userId()).isEqualTo(USER);
    }

    @Test
    void reuses_an_existing_admin_role_without_defining_a_new_one() {
        RoleId existingRoleId = new RoleId(UUID.randomUUID());
        List<AssignRoleRequest> assigned = new ArrayList<>();
        DefineRoleUseCase defineRole = request -> {
            throw new AssertionError("must not define a role that already exists");
        };
        AssignApplicationAdministratorUseCaseImpl useCase = new AssignApplicationAdministratorUseCaseImpl(
                roleLookupResolving(existingRoleId), defineRole, assignRoleSucceeding(assigned));

        StepVerifier.create(useCase.execute(REQUEST)).assertNext(response -> {
            assertThat(response.roleId()).isEqualTo(existingRoleId);
        }).verifyComplete();

        assertThat(assigned).hasSize(1);
        assertThat(assigned.getFirst().roleId()).isEqualTo(existingRoleId);
    }

    private static DefineRoleUseCase defineRoleSucceeding(List<DefineRoleRequest> received, RoleId assignedId) {
        return request -> {
            received.add(request);
            return Mono.just(new RoleResponse(assignedId, request.name(), request.scope(), Set.of(), REGISTERED_AT));
        };
    }

    private static AssignRoleUseCase assignRoleSucceeding(List<AssignRoleRequest> received) {
        return request -> {
            received.add(request);
            return Mono.just(new AssignmentResponse(new AssignmentId(UUID.randomUUID()), request.userId(),
                    request.tenantId(), request.applicationId(), request.roleId(), REGISTERED_AT, Optional.empty()));
        };
    }

    private static RoleLookupByNameInScopeValidator roleLookupResolving(RoleId found) {
        return query -> found == null ? Mono.empty() : Mono.just(found);
    }
}
