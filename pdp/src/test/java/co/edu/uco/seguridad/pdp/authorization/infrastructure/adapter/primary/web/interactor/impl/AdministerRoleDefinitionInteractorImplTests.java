package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerRoleDefinitionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerRoleDefinitionUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.DefineRoleRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.RoleAdministrationWebResponse;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-016: construye la solicitud de administración solo cuando el rol es de alcance APPLICATION —
 * el applicationId sale del propio scope de la petición, nunca de una consulta adicional (a
 * diferencia de {@code AdministerResourceGrantInteractorImpl}, que sí necesita resolverlo del rol
 * ya existente).
 */
class AdministerRoleDefinitionInteractorImplTests {

    private static final String TENANT = "universidad-uco";
    private static final String SUBJECT = "keycloak-subject-123";
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final RoleId ROLE_ID = new RoleId(UUID.randomUUID());
    private static final String APPLICATION_ID = UUID.randomUUID().toString();
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-14T00:00:00Z");

    @Test
    void builds_administration_present_for_an_application_scoped_role() {
        List<AdministerRoleDefinitionRequest> received = new ArrayList<>();
        RoleResponse response = new RoleResponse(ROLE_ID, new RoleName("Docente"),
                RoleScope.ofApplication(new co.edu.uco.seguridad.pdp.commons.model.TenantId(TENANT),
                        co.edu.uco.seguridad.pdp.commons.model.ApplicationId.of(APPLICATION_ID)),
                Set.of(), REGISTERED_AT);
        AdministerRoleDefinitionInteractorImpl interactor = new AdministerRoleDefinitionInteractorImpl(
                useCaseCapturing(received, response), subjectUserIdLookup());
        DefineRoleRawRequest raw = new DefineRoleRawRequest("Docente", "APPLICATION", APPLICATION_ID);

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT)))
                .assertNext(webResponse -> assertThat(webResponse.id()).isEqualTo(ROLE_ID.value().toString()))
                .verifyComplete();

        assertThat(received).hasSize(1);
        Optional<co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest> administration =
                received.getFirst().administration();
        assertThat(administration).isPresent();
        assertThat(administration.orElseThrow().applicationId().value().toString()).isEqualTo(APPLICATION_ID);
        assertThat(administration.orElseThrow().subjectUserId()).isEqualTo(USER);
        assertThat(administration.orElseThrow().tenantId().value()).isEqualTo(TENANT);
    }

    @Test
    void builds_administration_empty_for_a_tenant_scoped_role() {
        List<AdministerRoleDefinitionRequest> received = new ArrayList<>();
        RoleResponse response = new RoleResponse(ROLE_ID, new RoleName("Coordinador"),
                RoleScope.ofTenant(new co.edu.uco.seguridad.pdp.commons.model.TenantId(TENANT)), Set.of(), REGISTERED_AT);
        AdministerRoleDefinitionInteractorImpl interactor = new AdministerRoleDefinitionInteractorImpl(
                useCaseCapturing(received, response), subjectUserIdLookup());
        DefineRoleRawRequest raw = new DefineRoleRawRequest("Coordinador", "TENANT", null);

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT)))
                .assertNext(webResponse -> assertThat(webResponse.id()).isEqualTo(ROLE_ID.value().toString()))
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().administration()).isEqualTo(Optional.empty());
    }

    private static SubjectUserIdLookupValidator subjectUserIdLookup() {
        return subject -> Mono.just(USER);
    }

    private static AdministerRoleDefinitionUseCase useCaseCapturing(List<AdministerRoleDefinitionRequest> received,
            RoleResponse response) {
        return input -> {
            received.add(input);
            return Mono.just(response);
        };
    }
}
