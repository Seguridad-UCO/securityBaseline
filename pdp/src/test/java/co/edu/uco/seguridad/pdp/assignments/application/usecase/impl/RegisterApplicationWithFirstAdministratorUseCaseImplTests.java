package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RegisterApplicationWithFirstAdministratorRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignRoleUseCase;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.usecase.DefineRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La orquestación no decide nada de negocio propio: registra, define el rol ADMIN de la aplicación
 * recién creada y lo asigna al registrador, en ese orden — mismo espíritu que
 * RegisterApplicationWithInitialResourceUseCaseImplTests (HU-010).
 */
class RegisterApplicationWithFirstAdministratorUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION_ID = new ApplicationId(UUID.randomUUID());
    private static final ApplicationName NAME = new ApplicationName("gestion-academica");
    private static final ApplicationBaseUrl BASE_URL = new ApplicationBaseUrl("https://example.com");
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-13T00:00:00Z");
    private static final UserId REGISTRAR = new UserId(UUID.randomUUID());
    private static final RoleId ADMIN_ROLE_ID = new RoleId(UUID.randomUUID());
    private static final RegisterApplicationRequest APPLICATION_REQUEST =
            new RegisterApplicationRequest(TENANT, NAME, "Sistema académico", BASE_URL);
    private static final RegisterApplicationWithFirstAdministratorRequest REQUEST =
            new RegisterApplicationWithFirstAdministratorRequest(APPLICATION_REQUEST, REGISTRAR);

    @Test
    void registers_the_application_defines_its_admin_role_and_assigns_it_to_the_registrar() {
        List<DefineRoleRequest> definedRoles = new ArrayList<>();
        List<AssignRoleRequest> assignedRoles = new ArrayList<>();
        RegisterApplicationWithFirstAdministratorUseCaseImpl useCase = new RegisterApplicationWithFirstAdministratorUseCaseImpl(
                registerApplicationSucceeding(), defineRoleSucceeding(definedRoles), assignRoleSucceeding(assignedRoles));

        StepVerifier.create(useCase.execute(REQUEST))
                .assertNext(response -> {
                    assertThat(response.credential()).isEqualTo("secreto-en-claro");
                    assertThat(response.application().id()).isEqualTo(APPLICATION_ID);
                })
                .verifyComplete();

        assertThat(definedRoles).hasSize(1);
        assertThat(definedRoles.getFirst().name()).isEqualTo(new RoleName("ADMIN"));
        assertThat(definedRoles.getFirst().scope()).isEqualTo(RoleScope.ofApplication(TENANT, APPLICATION_ID));

        assertThat(assignedRoles).hasSize(1);
        assertThat(assignedRoles.getFirst().tenantId()).isEqualTo(TENANT);
        assertThat(assignedRoles.getFirst().userId()).isEqualTo(REGISTRAR);
        assertThat(assignedRoles.getFirst().applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(assignedRoles.getFirst().roleId()).isEqualTo(ADMIN_ROLE_ID);
    }

    @Test
    void never_defines_a_role_or_assigns_it_when_the_application_registration_fails() {
        RuntimeException failure = new RuntimeException("nombre reservado");
        RegisterApplicationUseCase registerApplication = dto -> Mono.error(failure);
        DefineRoleUseCase defineRole = request -> { throw new AssertionError("must not reach role definition"); };
        AssignRoleUseCase assignRole = request -> { throw new AssertionError("must not reach role assignment"); };
        RegisterApplicationWithFirstAdministratorUseCaseImpl useCase = new RegisterApplicationWithFirstAdministratorUseCaseImpl(
                registerApplication, defineRole, assignRole);

        StepVerifier.create(useCase.execute(REQUEST)).expectErrorMessage("nombre reservado").verify();
    }

    private static RegisterApplicationUseCase registerApplicationSucceeding() {
        return dto -> Mono.just(new ApplicationRegistrationResponse(
                new RegisteredApplicationResponse(APPLICATION_ID, TENANT, NAME, "Sistema académico", BASE_URL,
                        REGISTERED_AT),
                "secreto-en-claro"));
    }

    private static DefineRoleUseCase defineRoleSucceeding(List<DefineRoleRequest> received) {
        return request -> {
            received.add(request);
            return Mono.just(new RoleResponse(ADMIN_ROLE_ID, request.name(), request.scope(), java.util.Set.of(),
                    REGISTERED_AT));
        };
    }

    private static AssignRoleUseCase assignRoleSucceeding(List<AssignRoleRequest> received) {
        return request -> {
            received.add(request);
            return Mono.just(new AssignmentResponse(new AssignmentId(UUID.randomUUID()), request.userId(),
                    request.tenantId(), request.applicationId(), request.roleId(), REGISTERED_AT, Optional.empty()));
        };
    }
}
