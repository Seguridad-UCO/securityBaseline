package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationNameForTenantLookupValidator;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustAdministerAtLeastOneApplicationValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse.BulkResourceRowError;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.domain.Profile;
import co.edu.uco.seguridad.pdp.profiles.domain.ProfileCriteria;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modalidad {@code resources-roles}: recursos, roles y vínculos en un solo archivo, todo o nada.
 */
class BulkResourceRoleImportServiceTests {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");
    private static final TenantId TENANT = new TenantId("tenant-a");
    private static final UserId ACTOR = new UserId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
    private static final PdpPrincipal PRINCIPAL = new PdpPrincipal(TENANT, "subject-1", "jti-1",
            Optional.of(ACTOR), AuthenticationContextEvidence.NONE);
    private static final ApplicationId APP_UNO = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private static final ResourceId PERSISTED_REPORTS = new ResourceId(UUID.fromString("00000000-0000-0000-0000-0000000000b1"));
    private static final String[] RESOURCE_HEADERS = {"Aplicación*", "Ruta*", "Método HTTP*"};

    private final List<String> writes = new ArrayList<>();
    private final List<ProtectedResource> registered = new ArrayList<>();
    private final List<Role> savedRoles = new ArrayList<>();
    private final List<ResourceId> deleted = new ArrayList<>();
    private boolean gateAllows = true;
    private boolean rolesFail = false;

    // ---- escenarios -------------------------------------------------------------------------------------------

    @Test
    void a_clean_file_creates_the_resources_first_and_then_the_roles_linked_to_them() throws Exception {
        byte[] file = workbook(
                new String[]{"App Uno", "/orders", "GET"},
                new String[]{"Editor", "APPLICATION", "App Uno"},
                new String[]{"Editor", "APPLICATION", "App Uno", "/orders", "GET"});

        StepVerifier.create(service().commit(file, PRINCIPAL, ACTOR))
                .assertNext(response -> {
                    assertThat(response.ready()).isTrue();
                    assertThat(response.totalRows()).isEqualTo(2);
                    assertThat(response.validRows()).isEqualTo(2);
                })
                .verifyComplete();

        assertThat(writes).containsExactly("resources", "roles");
        assertThat(registered).hasSize(1);
        assertThat(savedRoles).hasSize(1);
        assertThat(savedRoles.get(0).resources()).containsExactly(registered.get(0).id());
    }

    @Test
    void a_role_can_also_be_linked_to_an_already_persisted_resource() throws Exception {
        byte[] file = workbook(
                new String[]{"App Uno", "/orders", "GET"},
                new String[]{"Editor", "APPLICATION", "App Uno"},
                new String[]{"Editor", "APPLICATION", "App Uno", "/reports", "GET"});

        StepVerifier.create(service().commit(file, PRINCIPAL, ACTOR))
                .assertNext(response -> assertThat(response.ready()).isTrue())
                .verifyComplete();

        assertThat(savedRoles.get(0).resources()).containsExactly(PERSISTED_REPORTS);
    }

    @Test
    void validating_never_persists() throws Exception {
        byte[] file = workbook(
                new String[]{"App Uno", "/orders", "GET"},
                new String[]{"Editor", "APPLICATION", "App Uno"},
                new String[]{"Editor", "APPLICATION", "App Uno", "/orders", "GET"});

        StepVerifier.create(service().validate(file, PRINCIPAL, ACTOR))
                .assertNext(response -> assertThat(response.ready()).isTrue())
                .verifyComplete();

        assertThat(writes).isEmpty();
    }

    @Test
    void a_broken_relation_blocks_the_whole_file_and_nothing_is_saved() throws Exception {
        byte[] file = workbook(
                new String[]{"App Uno", "/orders", "GET"},
                new String[]{"Editor", "APPLICATION", "App Uno"},
                new String[]{"Editor", "APPLICATION", "App Uno", "/no-existe", "GET"});

        StepVerifier.create(service().commit(file, PRINCIPAL, ACTOR))
                .assertNext(response -> {
                    assertThat(response.ready()).isFalse();
                    assertThat(response.errors()).extracting(BulkResourceRowError::code).containsExactly("INVALID_REFERENCE");
                })
                .verifyComplete();

        assertThat(writes).isEmpty();
    }

    @Test
    void without_administering_the_application_neither_resources_nor_roles_are_accepted() throws Exception {
        gateAllows = false;
        byte[] file = workbook(
                new String[]{"App Uno", "/orders", "GET"},
                new String[]{"Editor", "APPLICATION", "App Uno"},
                new String[]{"Editor", "APPLICATION", "App Uno", "/orders", "GET"});

        StepVerifier.create(service().commit(file, PRINCIPAL, ACTOR))
                .assertNext(response -> {
                    assertThat(response.ready()).isFalse();
                    assertThat(response.errors()).extracting(BulkResourceRowError::field, BulkResourceRowError::code)
                            .contains(org.assertj.core.groups.Tuple.tuple("Recursos · row", "NOT_AUTHORIZED"),
                                    org.assertj.core.groups.Tuple.tuple("Aplicación", "NOT_AUTHORIZED"));
                })
                .verifyComplete();

        assertThat(writes).isEmpty();
    }

    @Test
    void if_the_roles_cannot_be_saved_the_new_resources_are_removed_and_the_error_propagates() throws Exception {
        rolesFail = true;
        byte[] file = workbook(
                new String[]{"App Uno", "/orders", "GET"},
                new String[]{"Editor", "APPLICATION", "App Uno"},
                new String[]{"Editor", "APPLICATION", "App Uno", "/orders", "GET"});

        StepVerifier.create(service().commit(file, PRINCIPAL, ACTOR))
                .expectErrorMessage("fallo al guardar roles")
                .verify();

        assertThat(registered).hasSize(1);
        assertThat(deleted).containsExactly(registered.get(0).id());
        assertThat(savedRoles).isEmpty();
    }

    @Test
    void the_mode_is_registered_and_unknown_modes_are_rejected() {
        BulkCatalogImports imports = new BulkCatalogImports(java.util.Map.of("resources-roles", service()));

        assertThat(imports.forMode("resources-roles")).isNotNull();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> imports.forMode("otra"))
                .isInstanceOf(UnsupportedBulkModeException.class)
                .hasMessageContaining("resources-roles");
    }

    // ---- dobles -----------------------------------------------------------------------------------------------

    private BulkResourceRoleImportService service() {
        AtomicInteger sequence = new AtomicInteger();
        ProtectedResourceRepository resources = new ProtectedResourceRepository() {
            @Override
            public Mono<Boolean> existsByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path, HttpVerb method) {
                return Mono.just(false);
            }

            @Override
            public Flux<ProtectedResource> findAllByApplication(ApplicationId applicationId) {
                return Flux.empty();
            }

            @Override
            public Mono<ProtectedResource> save(ProtectedResource resource) {
                return Mono.error(new UnsupportedOperationException());
            }

            @Override
            public Mono<Void> deleteById(ResourceId resourceId) {
                return Mono.fromRunnable(() -> deleted.add(resourceId));
            }

            @Override
            public Mono<ApplicationId> findApplicationIdById(ResourceId resourceId) {
                return Mono.empty();
            }

            @Override
            public Mono<ResourceId> findIdByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path, HttpVerb method) {
                return "/reports".equals(path.value()) ? Mono.just(PERSISTED_REPORTS) : Mono.empty();
            }
        };
        RoleRepository roles = new RoleRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(RoleName name, RoleScope scope) {
                return Mono.just(false);
            }

            @Override
            public Mono<Role> findByNameInScope(RoleName name, RoleScope scope) {
                return Mono.empty();
            }

            @Override
            public Mono<Role> findByIdForTenant(RoleId roleId, TenantId tenantId) {
                return Mono.empty();
            }

            @Override
            public Mono<Role> findById(RoleId roleId) {
                return Mono.empty();
            }

            @Override
            public Mono<ResultPage<Role>> findBy(RoleCriteria criteria, PageWindow window) {
                return Mono.error(new UnsupportedOperationException());
            }

            @Override
            public Mono<Role> save(Role role) {
                return Mono.error(new UnsupportedOperationException());
            }

            @Override
            public Mono<Void> saveAllAtomically(List<Role> batch) {
                if (rolesFail) return Mono.error(new IllegalStateException("fallo al guardar roles"));
                return Mono.fromRunnable(() -> {
                    writes.add("roles");
                    savedRoles.addAll(batch);
                });
            }
        };
        ProfileRepository profiles = new ProfileRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(ProfileName name, RoleScope scope) {
                return Mono.error(new UnsupportedOperationException());
            }

            @Override
            public Mono<Profile> findByIdForTenant(ProfileId profileId, TenantId tenantId) {
                return Mono.error(new UnsupportedOperationException());
            }

            @Override
            public Mono<ResultPage<Profile>> findBy(ProfileCriteria criteria, PageWindow window) {
                return Mono.error(new UnsupportedOperationException());
            }

            @Override
            public Mono<Profile> save(Profile profile) {
                return Mono.error(new UnsupportedOperationException());
            }
        };
        ApplicationNameForTenantLookupValidator applications = query -> query.name().value().equals("App Uno")
                ? Mono.just(new RegisteredApplicationResponse(APP_UNO, query.tenantId(), query.name(), "",
                new ApplicationBaseUrl("https://app.example.com"), NOW))
                : Mono.empty();
        PrincipalMustBeApplicationAdministratorValidator administrators = request -> gateAllows
                ? Mono.empty()
                : Mono.error(new NotAuthorizedToAdministerException(request.tenantId(), request.applicationId()));
        PrincipalMustAdministerAtLeastOneApplicationValidator tenantAdministrators = request -> Mono.empty();

        BulkCatalogImportService catalog = new BulkCatalogImportService(new BulkCatalogWorkbookService(), roles, profiles,
                resources, applications, administrators, tenantAdministrators, () -> new UUID(0, sequence.incrementAndGet()), () -> NOW);
        BulkResourceImportService resourceImports = new BulkResourceImportService(new BulkResourceWorkbookService(),
                applications, administrators, resources, () -> new UUID(0, sequence.incrementAndGet()), () -> NOW,
                batch -> Mono.error(new IllegalStateException("la validación no debe registrar")));
        return new BulkResourceRoleImportService(resourceImports, catalog, batch -> Mono.fromRunnable(() -> {
            writes.add("resources");
            registered.addAll(batch);
        }), resources, roles);
    }

    private static byte[] workbook(String[] resource, String[] role, String[] relation) throws Exception {
        return CatalogWorkbooks.xlsx(
                CatalogWorkbooks.sheet("Recursos", RESOURCE_HEADERS, (Object[]) resource),
                CatalogWorkbooks.roles((Object[]) role),
                CatalogWorkbooks.roleResources((Object[]) relation));
    }
}
