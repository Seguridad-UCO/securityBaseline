package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustAdministerAtLeastOneApplicationValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.domain.exception.ProfileNotFoundException;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.domain.exception.RoleNotFoundException;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-036: {@code PATCH}/{@code DELETE} de roles y perfiles. Un objetivo {@code TENANT} (el lookup de aplicación
 * viene vacío) exige el actor de inquilino; uno {@code APPLICATION}, administrar esa aplicación. La existencia
 * del rol/perfil se comprueba antes de cualquier gate (404, no 403). Además cierra un fail-open previo: un
 * principal cuyo {@code UserId} no se puede resolver ya no se salta el gate — se le deniega.
 *
 * <p>El controlador se construye a mano, con lambdas; las operaciones se anotan al <em>suscribirse</em>, de
 * modo que «la escritura no ocurre» significa que nadie llegó a suscribir la operación.</p>
 */
class CatalogMaintenanceControllerTests {

    private static final String TENANT_VALUE = "universidad-uco";
    private static final TenantId TENANT = new TenantId(TENANT_VALUE);
    private static final String SUBJECT = "test-subject";
    private static final UserId USER = new UserId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private static final ApplicationId GRANTING_APPLICATION = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a2"));
    private static final RoleId ROLE = new RoleId(UUID.fromString("00000000-0000-0000-0000-0000000000c1"));
    private static final ProfileId PROFILE = new ProfileId(UUID.fromString("00000000-0000-0000-0000-0000000000d1"));
    private static final ResourceId RESOURCE = new ResourceId(UUID.fromString("00000000-0000-0000-0000-0000000000b1"));
    private static final Instant NOW = Instant.parse("2026-10-30T12:00:00Z");
    private static final RoleResponse ROLE_RESPONSE = new RoleResponse(ROLE, new RoleName("Coordinador"),
            RoleScope.ofTenant(TENANT), Set.of(), NOW);
    private static final ProfileResponse PROFILE_RESPONSE = new ProfileResponse(PROFILE, new ProfileName("Coordinadores"),
            RoleScope.ofTenant(TENANT), Set.of(), NOW);

    private final List<AdministrationRequest> applicationGateCalls = new ArrayList<>();
    private final List<TenantAdministrationRequest> tenantGateCalls = new ArrayList<>();
    /** Lo que se llegó a suscribir de las escrituras (y de las comprobaciones de dependencias que las preceden). */
    private final List<String> writes = new ArrayList<>();

    private Mono<UserId> userLookup = Mono.just(USER);
    private Function<Object, Mono<Optional<ApplicationId>>> roleOwner = query -> Mono.just(Optional.empty());
    private Function<Object, Mono<Optional<ApplicationId>>> profileOwner = query -> Mono.just(Optional.empty());
    private Function<AdministrationRequest, Mono<Void>> applicationGate = request -> Mono.empty();
    private Function<TenantAdministrationRequest, Mono<ApplicationId>> tenantGate = actor -> Mono.just(GRANTING_APPLICATION);

    private CatalogMaintenanceController controller() {
        PrincipalMustBeApplicationAdministratorValidator administrator = request -> Mono.defer(() -> {
            applicationGateCalls.add(request);
            return applicationGate.apply(request);
        });
        PrincipalMustAdministerAtLeastOneApplicationValidator tenantAdministrator = actor -> Mono.defer(() -> {
            tenantGateCalls.add(actor);
            return tenantGate.apply(actor);
        });
        SubjectUserIdLookupValidator users = subject -> Mono.defer(() -> userLookup);
        return new CatalogMaintenanceController(administrator, tenantAdministrator, users,
                request -> recorded("updateApplication", Mono.empty()),
                request -> recorded("updateResource", Mono.empty()),
                request -> recorded("deleteResource", Mono.empty()),
                request -> recorded("updateRole", Mono.just(ROLE_RESPONSE)),
                request -> recorded("deleteRole", Mono.empty()),
                request -> recorded("updateProfile", Mono.just(PROFILE_RESPONSE)),
                request -> recorded("deleteProfile", Mono.empty()),
                query -> Mono.defer(() -> roleOwner.apply(query)),
                query -> Mono.defer(() -> profileOwner.apply(query)),
                resource -> recorded("resourceDependencies", Mono.empty()),
                role -> recorded("profileRoleDependencies", Mono.empty()),
                role -> recorded("assignmentRoleDependencies", Mono.empty()),
                profile -> recorded("profileDependencies", Mono.empty()));
    }

    private <T> Mono<T> recorded(String write, Mono<T> result) {
        return Mono.defer(() -> {
            writes.add(write);
            return result;
        });
    }

    private static MockServerWebExchange exchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.patch("/api/v1/roles/x").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }

    private static <T> StepVerifier.FirstStep<T> authenticated(Mono<T> route) {
        return StepVerifier.create(route.contextWrite(TestJwtSupport.withPrincipal(TENANT_VALUE, SUBJECT)));
    }

    private static TenantAdministrationRequest expectedActor() {
        return new TenantAdministrationRequest(TENANT, USER, SUBJECT,
                new co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence(Optional.empty(), List.of()));
    }

    private void assertNothingWasWritten() {
        assertThat(writes).isEmpty();
    }

    // ---- roles TENANT --------------------------------------------------------------------------------------------

    @Test
    void renaming_a_tenant_role_requires_the_tenant_gate() {
        CatalogMaintenanceController controller = controller();

        authenticated(controller.updateRole(ROLE.value().toString(), new CatalogMaintenanceController.NamePatch("Coordinador"), exchange()))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody().code()).isEqualTo("ROLE_UPDATED");
                    assertThat(response.getBody().data()).isEqualTo(ROLE_RESPONSE);
                })
                .verifyComplete();

        assertThat(tenantGateCalls).containsExactly(expectedActor());
        assertThat(applicationGateCalls).isEmpty();
        assertThat(writes).containsExactly("updateRole");
    }

    @Test
    void renaming_a_tenant_role_is_refused_without_writing_when_the_tenant_gate_denies() {
        tenantGate = actor -> Mono.error(new NotAuthorizedToAdministerTenantCatalogException(TENANT, Optional.of(APPLICATION)));

        authenticated(controller().updateRole(ROLE.value().toString(), new CatalogMaintenanceController.NamePatch("Coordinador"), exchange()))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();

        assertThat(tenantGateCalls).hasSize(1);
        assertNothingWasWritten();
    }

    @Test
    void deleting_a_tenant_role_requires_the_tenant_gate() {
        authenticated(controller().deleteRole(ROLE.value().toString(), exchange()))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody().code()).isEqualTo("ROLE_REMOVED");
                })
                .verifyComplete();

        assertThat(tenantGateCalls).containsExactly(expectedActor());
        assertThat(applicationGateCalls).isEmpty();
        assertThat(writes).contains("deleteRole");
    }

    @Test
    void deleting_a_tenant_role_is_refused_without_writing_when_the_tenant_gate_denies() {
        tenantGate = actor -> Mono.error(new NotAuthorizedToAdministerTenantCatalogException(TENANT, Optional.of(APPLICATION)));

        authenticated(controller().deleteRole(ROLE.value().toString(), exchange()))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();

        assertThat(tenantGateCalls).hasSize(1);
        // ni la borra ni siquiera consulta las dependencias: el gate va primero
        assertNothingWasWritten();
    }

    // ---- perfiles TENANT -----------------------------------------------------------------------------------------

    @Test
    void renaming_a_tenant_profile_requires_the_tenant_gate() {
        authenticated(controller().updateProfile(PROFILE.value().toString(), new CatalogMaintenanceController.NamePatch("Coordinadores"), exchange()))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody().code()).isEqualTo("PROFILE_UPDATED");
                    assertThat(response.getBody().data()).isEqualTo(PROFILE_RESPONSE);
                })
                .verifyComplete();

        assertThat(tenantGateCalls).containsExactly(expectedActor());
        assertThat(applicationGateCalls).isEmpty();
        assertThat(writes).containsExactly("updateProfile");
    }

    @Test
    void renaming_a_tenant_profile_is_refused_without_writing_when_the_tenant_gate_denies() {
        tenantGate = actor -> Mono.error(new NotAuthorizedToAdministerTenantCatalogException(TENANT, Optional.empty()));

        authenticated(controller().updateProfile(PROFILE.value().toString(), new CatalogMaintenanceController.NamePatch("Coordinadores"), exchange()))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();

        assertNothingWasWritten();
    }

    @Test
    void deleting_a_tenant_profile_requires_the_tenant_gate() {
        authenticated(controller().deleteProfile(PROFILE.value().toString(), exchange()))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody().code()).isEqualTo("PROFILE_REMOVED");
                })
                .verifyComplete();

        assertThat(tenantGateCalls).containsExactly(expectedActor());
        assertThat(applicationGateCalls).isEmpty();
        assertThat(writes).contains("deleteProfile");
    }

    @Test
    void deleting_a_tenant_profile_is_refused_without_writing_when_the_tenant_gate_denies() {
        tenantGate = actor -> Mono.error(new NotAuthorizedToAdministerTenantCatalogException(TENANT, Optional.of(APPLICATION)));

        authenticated(controller().deleteProfile(PROFILE.value().toString(), exchange()))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();

        assertNothingWasWritten();
    }

    // ---- alcance APPLICATION (sin regresión) ---------------------------------------------------------------------

    @Test
    void an_application_role_uses_only_the_application_gate() {
        roleOwner = query -> Mono.just(Optional.of(APPLICATION));

        authenticated(controller().updateRole(ROLE.value().toString(), new CatalogMaintenanceController.NamePatch("Docente"), exchange()))
                .assertNext(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();

        assertThat(applicationGateCalls).containsExactly(new AdministrationRequest(TENANT, APPLICATION, USER, SUBJECT,
                Set.of(), expectedActor().authenticationContext()));
        assertThat(tenantGateCalls).isEmpty();
        assertThat(writes).containsExactly("updateRole");
    }

    @Test
    void an_application_profile_uses_only_the_application_gate() {
        profileOwner = query -> Mono.just(Optional.of(APPLICATION));

        authenticated(controller().deleteProfile(PROFILE.value().toString(), exchange()))
                .assertNext(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();

        assertThat(applicationGateCalls).containsExactly(new AdministrationRequest(TENANT, APPLICATION, USER, SUBJECT,
                Set.of(), expectedActor().authenticationContext()));
        assertThat(tenantGateCalls).isEmpty();
    }

    @Test
    void an_application_role_that_the_principal_does_not_administer_is_refused_without_writing() {
        roleOwner = query -> Mono.just(Optional.of(APPLICATION));
        applicationGate = request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, request.applicationId()));

        authenticated(controller().deleteRole(ROLE.value().toString(), exchange()))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();

        assertThat(tenantGateCalls).isEmpty();
        assertNothingWasWritten();
    }

    // ---- 404 antes que 403 ---------------------------------------------------------------------------------------

    @Test
    void a_missing_role_is_404_before_any_gate() {
        roleOwner = query -> Mono.error(new RoleNotFoundException(ROLE));
        CatalogMaintenanceController controller = controller();

        authenticated(controller.updateRole(ROLE.value().toString(), new CatalogMaintenanceController.NamePatch("Coordinador"), exchange()))
                .expectError(RoleNotFoundException.class)
                .verify();
        authenticated(controller.deleteRole(ROLE.value().toString(), exchange()))
                .expectError(RoleNotFoundException.class)
                .verify();

        assertThat(tenantGateCalls).isEmpty();
        assertThat(applicationGateCalls).isEmpty();
        assertNothingWasWritten();
    }

    @Test
    void a_missing_profile_is_404_before_any_gate() {
        profileOwner = query -> Mono.error(new ProfileNotFoundException(PROFILE));
        CatalogMaintenanceController controller = controller();

        authenticated(controller.updateProfile(PROFILE.value().toString(), new CatalogMaintenanceController.NamePatch("Coordinadores"), exchange()))
                .expectError(ProfileNotFoundException.class)
                .verify();
        authenticated(controller.deleteProfile(PROFILE.value().toString(), exchange()))
                .expectError(ProfileNotFoundException.class)
                .verify();

        assertThat(tenantGateCalls).isEmpty();
        assertThat(applicationGateCalls).isEmpty();
        assertNothingWasWritten();
    }

    // ---- fail-closed ante un UserId irresoluble (cierra el fail-open previo de §6.5) -----------------------------

    @Test
    void an_unresolvable_user_id_denies_instead_of_skipping_the_gate_for_tenant_roles_and_profiles() {
        userLookup = Mono.empty();
        CatalogMaintenanceController controller = controller();

        authenticated(controller.updateRole(ROLE.value().toString(), new CatalogMaintenanceController.NamePatch("Coordinador"), exchange()))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class).verify();
        authenticated(controller.deleteRole(ROLE.value().toString(), exchange()))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class).verify();
        authenticated(controller.updateProfile(PROFILE.value().toString(), new CatalogMaintenanceController.NamePatch("Coordinadores"), exchange()))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class).verify();
        authenticated(controller.deleteProfile(PROFILE.value().toString(), exchange()))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class).verify();

        assertThat(tenantGateCalls).isEmpty();
        assertNothingWasWritten();
    }

    @Test
    void an_unresolvable_user_id_denies_instead_of_skipping_the_gate_for_application_roles_and_profiles() {
        userLookup = Mono.empty();
        roleOwner = query -> Mono.just(Optional.of(APPLICATION));
        profileOwner = query -> Mono.just(Optional.of(APPLICATION));
        CatalogMaintenanceController controller = controller();

        authenticated(controller.updateRole(ROLE.value().toString(), new CatalogMaintenanceController.NamePatch("Docente"), exchange()))
                .expectError(NotAuthorizedToAdministerException.class).verify();
        authenticated(controller.deleteRole(ROLE.value().toString(), exchange()))
                .expectError(NotAuthorizedToAdministerException.class).verify();
        authenticated(controller.updateProfile(PROFILE.value().toString(), new CatalogMaintenanceController.NamePatch("Docentes"), exchange()))
                .expectError(NotAuthorizedToAdministerException.class).verify();
        authenticated(controller.deleteProfile(PROFILE.value().toString(), exchange()))
                .expectError(NotAuthorizedToAdministerException.class).verify();

        assertThat(applicationGateCalls).isEmpty();
        assertNothingWasWritten();
    }

    @Test
    void an_unresolvable_user_id_also_denies_the_application_and_resource_maintenance() {
        userLookup = Mono.empty();
        CatalogMaintenanceController controller = controller();
        String application = APPLICATION.value().toString();

        authenticated(controller.updateApplication(application, new CatalogMaintenanceController.ApplicationPatch(
                        "App Uno", "descripcion", "https://app.example.com", null), exchange()))
                .expectError(NotAuthorizedToAdministerException.class).verify();
        authenticated(controller.updateResource(application, RESOURCE.value().toString(),
                        new CatalogMaintenanceController.ResourcePatch("/api/orders", "GET"), exchange()))
                .expectError(NotAuthorizedToAdministerException.class).verify();
        authenticated(controller.deleteResource(application, RESOURCE.value().toString(), exchange()))
                .expectError(NotAuthorizedToAdministerException.class).verify();

        assertThat(applicationGateCalls).isEmpty();
        assertNothingWasWritten();
    }
}
