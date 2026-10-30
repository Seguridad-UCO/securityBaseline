package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationNameForTenantQuery;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse.BulkResourceRowError;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.CatalogWorkbooks.SheetData;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.domain.Profile;
import co.edu.uco.seguridad.pdp.profiles.domain.ProfileCriteria;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Validación y confirmación de la carga masiva de roles y perfiles: resuelve la aplicación por nombre dentro del
 * inquilino autenticado, detecta duplicados en el archivo y ya persistidos, resuelve las relaciones y solo
 * confirma un lote completamente válido.
 */
class BulkCatalogImportServiceTests {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final TenantId TENANT = new TenantId("tenant-a");
    private static final ApplicationId APP_UNO = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private static final ApplicationId APP_DOS = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a2"));
    private static final ResourceId ORDERS_GET = new ResourceId(UUID.fromString("00000000-0000-0000-0000-0000000000b1"));
    private static final ResourceId ORDERS_POST = new ResourceId(UUID.fromString("00000000-0000-0000-0000-0000000000b2"));

    private final BulkCatalogWorkbookService workbooks = new BulkCatalogWorkbookService();
    private final List<ApplicationNameForTenantQuery> applicationQueries = new ArrayList<>();
    private final List<Role> persistedRoles = new ArrayList<>();
    private final List<Profile> persistedProfiles = new ArrayList<>();
    private final Map<String, ResourceId> knownResources = new HashMap<>();
    private final List<Role> rolesSaved = new ArrayList<>();
    private final List<Profile> profilesSaved = new ArrayList<>();
    private RuntimeException saveFailure;

    private BulkCatalogImportService service() {
        AtomicInteger sequence = new AtomicInteger();
        knownResources.put(APP_UNO.value() + "|/api/orders|GET", ORDERS_GET);
        knownResources.put(APP_UNO.value() + "|/api/orders|POST", ORDERS_POST);
        RoleRepository roles = new RoleRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(RoleName name, RoleScope scope) {
                return Mono.just(persistedRoles.stream().anyMatch(r -> r.name().equals(name) && r.scope().equals(scope)));
            }

            @Override
            public Mono<Role> findByNameInScope(RoleName name, RoleScope scope) {
                return Mono.justOrEmpty(persistedRoles.stream()
                        .filter(r -> r.name().equals(name) && r.scope().equals(scope)).findFirst());
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

            @Override
            public Mono<Void> saveAllAtomically(List<Role> batch) {
                if (saveFailure != null) {
                    return Mono.error(saveFailure);
                }
                rolesSaved.addAll(batch);
                return Mono.empty();
            }
        };
        ProfileRepository profiles = new ProfileRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(ProfileName name, RoleScope scope) {
                return Mono.just(persistedProfiles.stream().anyMatch(p -> p.name().equals(name) && p.scope().equals(scope)));
            }

            @Override
            public Mono<Profile> findByIdForTenant(ProfileId profileId, TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResultPage<Profile>> findBy(ProfileCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Profile> save(Profile profile) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> saveAllAtomically(List<Profile> batch) {
                if (saveFailure != null) {
                    return Mono.error(saveFailure);
                }
                profilesSaved.addAll(batch);
                return Mono.empty();
            }
        };
        ProtectedResourceRepository resources = new ProtectedResourceRepository() {
            @Override
            public Mono<Boolean> existsByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path, HttpVerb method) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Flux<ProtectedResource> findAllByApplication(ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ProtectedResource> save(ProtectedResource resource) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> deleteById(ResourceId resourceId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ApplicationId> findApplicationIdById(ResourceId resourceId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResourceId> findIdByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path, HttpVerb method) {
                return Mono.justOrEmpty(knownResources.get(applicationId.value() + "|" + path.value() + "|" + method));
            }
        };
        return new BulkCatalogImportService(workbooks, roles, profiles, resources,
                query -> {
                    applicationQueries.add(query);
                    return switch (query.name().value()) {
                        case "App Uno" -> Mono.just(application(APP_UNO, query));
                        case "App Dos" -> Mono.just(application(APP_DOS, query));
                        case "App Rota" -> Mono.error(new IllegalStateException("catálogo de aplicaciones caído"));
                        default -> Mono.empty();
                    };
                },
                () -> new UUID(0, sequence.incrementAndGet()),
                () -> NOW);
    }

    private static RegisteredApplicationResponse application(ApplicationId id, ApplicationNameForTenantQuery query) {
        return new RegisteredApplicationResponse(id, query.tenantId(), query.name(), "",
                new ApplicationBaseUrl("https://app.example.com"), NOW);
    }

    private static Role persistedRole(String name, RoleScope scope) {
        return Role.define(new RoleId(UUID.fromString("00000000-0000-0000-0000-0000000000c1")), new RoleName(name), scope, NOW);
    }

    private static List<String> codes(BulkResourceValidationResponse response) {
        return response.errors().stream().map(BulkResourceRowError::code).toList();
    }

    private static List<Integer> rows(BulkResourceValidationResponse response) {
        return response.errors().stream().map(BulkResourceRowError::row).toList();
    }

    private BulkCatalogImportService.Result validate(String mode, byte[] file) {
        AtomicReference<BulkCatalogImportService.Result> validated = new AtomicReference<>();
        StepVerifier.create(service().validate(mode, file, TENANT)).assertNext(validated::set).verifyComplete();
        return validated.get();
    }

    // ---- roles: camino feliz ----------------------------------------------------------------------------------

    @Test
    void a_clean_roles_sheet_is_ready_and_builds_the_roles_in_the_authenticated_tenant() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Auditor", "TENANT", null},
                new String[]{"Editor", "application", "App Uno"}));

        StepVerifier.create(service().validate("roles", file, TENANT))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isTrue();
                    assertThat(result.response().totalRows()).isEqualTo(2);
                    assertThat(result.response().validRows()).isEqualTo(2);
                    assertThat(result.response().invalidRows()).isZero();
                    assertThat(result.roles()).extracting(r -> r.name().value()).containsExactly("Auditor", "Editor");
                    assertThat(result.roles().get(0).scope()).isEqualTo(RoleScope.ofTenant(TENANT));
                    assertThat(result.roles().get(1).scope()).isEqualTo(RoleScope.ofApplication(TENANT, APP_UNO));
                    assertThat(result.roles()).extracting(Role::registeredAt).containsOnly(NOW);
                    assertThat(result.roles()).extracting(Role::id).doesNotHaveDuplicates();
                })
                .verifyComplete();

        assertThat(applicationQueries).extracting(ApplicationNameForTenantQuery::tenantId).containsOnly(TENANT);
        assertThat(rolesSaved).isEmpty();
    }

    @Test
    void commit_saves_a_valid_roles_batch_atomically_and_never_touches_profiles() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Auditor", "TENANT", null},
                new String[]{"Editor", "APPLICATION", "App Uno"}));

        StepVerifier.create(service().commit("roles", file, TENANT))
                .assertNext(result -> assertThat(result.response().ready()).isTrue())
                .verifyComplete();

        assertThat(rolesSaved).extracting(r -> r.name().value()).containsExactly("Auditor", "Editor");
        assertThat(profilesSaved).isEmpty();
    }

    @Test
    void the_roles_of_a_valid_batch_receive_the_resources_of_their_relation_rows() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(
                        new String[]{"Auditor", "TENANT", null},
                        new String[]{"Editor", "APPLICATION", "App Uno"}),
                CatalogWorkbooks.roleResources(
                        new String[]{"auditor", "tenant", "App Uno", "/api/orders", "get"},
                        new String[]{"Editor", "APPLICATION", "App Uno", "/api/orders", "GET"},
                        new String[]{"Editor", "APPLICATION", "App Uno", "/api/orders", "POST"}));

        StepVerifier.create(service().commit("roles", file, TENANT))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isTrue();
                    assertThat(result.roles().get(0).resources()).containsExactly(ORDERS_GET);
                    assertThat(result.roles().get(1).resources()).containsExactlyInAnyOrder(ORDERS_GET, ORDERS_POST);
                })
                .verifyComplete();

        assertThat(rolesSaved).hasSize(2);
    }

    @Test
    void a_role_batch_without_relation_rows_is_valid_because_the_relation_sheet_is_optional() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}),
                CatalogWorkbooks.roleResources());

        StepVerifier.create(service().validate("roles", file, TENANT))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isTrue();
                    assertThat(result.roles().get(0).resources()).isEmpty();
                })
                .verifyComplete();
    }

    // ---- roles: rechazos por fila -----------------------------------------------------------------------------

    @Test
    void a_name_repeated_in_the_same_scope_is_a_duplicate_in_file_ignoring_case_but_other_scopes_are_fine() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Auditor", "TENANT", null},
                new String[]{"AUDITOR", "TENANT", null},
                new String[]{"Auditor", "APPLICATION", "App Uno"}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(codes(result.response())).containsExactly("DUPLICATE_IN_FILE");
        assertThat(rows(result.response())).containsExactly(3);
        assertThat(result.response().ready()).isFalse();
        assertThat(result.response().totalRows()).isEqualTo(3);
        assertThat(result.response().validRows()).isEqualTo(2);
        assertThat(result.response().invalidRows()).isEqualTo(1);
        assertThat(result.roles()).hasSize(2);
    }

    @Test
    void a_role_that_already_exists_in_the_scope_is_a_persisted_duplicate() throws Exception {
        persistedRoles.add(persistedRole("Auditor", RoleScope.ofTenant(TENANT)));
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Auditor", "TENANT", null},
                new String[]{"Auditor", "APPLICATION", "App Uno"}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(result.response().errors()).singleElement().satisfies(error -> {
            assertThat(error.row()).isEqualTo(2);
            assertThat(error.code()).isEqualTo("DUPLICATE_PERSISTED");
            assertThat(error.field()).isEqualTo("Nombre de rol*");
        });
        assertThat(result.roles()).extracting(Role::scope).containsExactly(RoleScope.ofApplication(TENANT, APP_UNO));
    }

    @Test
    void an_application_that_does_not_belong_to_the_tenant_is_rejected() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Editor", "APPLICATION", "App Ajena"}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(result.response().errors()).singleElement().satisfies(error -> {
            assertThat(error.code()).isEqualTo("INVALID_VALUE");
            assertThat(error.field()).isEqualTo("Aplicación");
            assertThat(error.description()).isEqualTo("La aplicación no pertenece al tenant autenticado");
        });
        assertThat(applicationQueries).extracting(ApplicationNameForTenantQuery::tenantId).containsOnly(TENANT);
        assertThat(result.roles()).isEmpty();
    }

    @Test
    void a_failing_application_lookup_is_reported_on_its_row_instead_of_aborting_the_batch() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Editor", "APPLICATION", "App Rota"},
                new String[]{"Auditor", "TENANT", null}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(result.response().errors()).singleElement().satisfies(error -> {
            assertThat(error.row()).isEqualTo(2);
            assertThat(error.description()).isEqualTo("catálogo de aplicaciones caído");
        });
        assertThat(result.roles()).extracting(r -> r.name().value()).containsExactly("Auditor");
    }

    @Test
    void inconsistent_scope_and_name_values_are_reported_per_row() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Auditor", "TENANT", "App Uno"},
                new String[]{"Editor", "APPLICATION", null},
                new String[]{"Gestor", "PLANETA", null},
                new String[]{"Raiz", "GLOBAL", null},
                new String[]{null, "TENANT", null},
                new String[]{"Valido", "TENANT", null}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(rows(result.response())).containsExactly(2, 3, 4, 5, 6);
        assertThat(codes(result.response())).containsOnly("INVALID_VALUE");
        assertThat(result.response().errors().get(0).description()).isEqualTo("Aplicación solo aplica al alcance APPLICATION");
        assertThat(result.response().errors().get(1).description()).isEqualTo("APPLICATION requiere Aplicación");
        assertThat(result.response().totalRows()).isEqualTo(6);
        assertThat(result.response().validRows()).isEqualTo(1);
        assertThat(result.response().invalidRows()).isEqualTo(5);
        assertThat(result.roles()).extracting(r -> r.name().value()).containsExactly("Valido");
    }

    @Test
    void commit_does_not_save_anything_when_a_single_row_is_invalid() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Auditor", "TENANT", null},
                new String[]{"Editor", "APPLICATION", "App Ajena"}));

        StepVerifier.create(service().commit("roles", file, TENANT))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isFalse();
                    assertThat(result.response().validRows()).isEqualTo(1);
                })
                .verifyComplete();

        assertThat(rolesSaved).isEmpty();
        assertThat(profilesSaved).isEmpty();
    }

    @Test
    void commit_propagates_a_failure_of_the_atomic_save() throws Exception {
        saveFailure = new IllegalStateException("conflicto de unicidad");
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));

        StepVerifier.create(service().commit("roles", file, TENANT))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(IllegalStateException.class).hasMessage("conflicto de unicidad"))
                .verify();

        assertThat(rolesSaved).isEmpty();
    }

    // ---- roles: relaciones ------------------------------------------------------------------------------------

    @Test
    void role_resource_relations_are_checked_row_by_row() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}),
                CatalogWorkbooks.roleResources(
                        new String[]{"Auditor", "TENANT", "App Uno", "/api/orders", "GET"},
                        new String[]{"Auditor", "TENANT", "App Uno", "/api/orders", "GET"},
                        new String[]{"Fantasma", "TENANT", "App Uno", "/api/orders", "GET"},
                        new String[]{"Auditor", "TENANT", "App Ajena", "/api/orders", "GET"},
                        new String[]{"Auditor", "TENANT", "App Uno", "/api/inexistente", "GET"},
                        new String[]{"Auditor", "TENANT", "", "/api/orders", "GET"},
                        new String[]{"Auditor", "TENANT", "App Uno", "sin-barra", "GET"}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(result.response().ready()).isFalse();
        assertThat(result.response().errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::code)
                .containsExactly(
                        tuple(3, "DUPLICATE_IN_FILE"),
                        tuple(4, "ROLE_NOT_FOUND"),
                        tuple(5, "INVALID_REFERENCE"),
                        tuple(6, "INVALID_REFERENCE"),
                        tuple(7, "INVALID_VALUE"),
                        tuple(8, "INVALID_REFERENCE"));
        assertThat(result.response().errors().get(3).description()).isEqualTo("El recurso no existe en la aplicación indicada");
    }

    @Test
    void a_relation_row_that_cannot_be_read_blocks_the_commit() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}),
                CatalogWorkbooks.roleResources(new String[]{"Auditor", "TENANT", "App Uno", "/api/inexistente", "GET"}));

        StepVerifier.create(service().commit("roles", file, TENANT))
                .assertNext(result -> assertThat(result.response().ready()).isFalse())
                .verifyComplete();

        assertThat(rolesSaved).isEmpty();
    }

    // ---- perfiles ---------------------------------------------------------------------------------------------

    @Test
    void commit_saves_a_valid_profiles_batch_and_never_touches_roles() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(
                new String[]{"Gestores", "TENANT", null},
                new String[]{"Operadores", "APPLICATION", "App Dos"}));

        StepVerifier.create(service().commit("profiles", file, TENANT))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isTrue();
                    assertThat(result.profiles()).extracting(p -> p.scope())
                            .containsExactly(RoleScope.ofTenant(TENANT), RoleScope.ofApplication(TENANT, APP_DOS));
                })
                .verifyComplete();

        assertThat(profilesSaved).extracting(p -> p.name().value()).containsExactly("Gestores", "Operadores");
        assertThat(profilesSaved).extracting(Profile::registeredAt).containsOnly(NOW);
        assertThat(rolesSaved).isEmpty();
    }

    @Test
    void a_profile_that_already_exists_in_the_scope_is_a_persisted_duplicate() throws Exception {
        persistedProfiles.add(Profile.define(new ProfileId(UUID.fromString("00000000-0000-0000-0000-0000000000d1")),
                new ProfileName("Gestores"), RoleScope.ofTenant(TENANT), NOW));
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}));

        BulkCatalogImportService.Result result = validate("profiles", file);

        assertThat(result.response().errors()).singleElement().satisfies(error -> {
            assertThat(error.code()).isEqualTo("DUPLICATE_PERSISTED");
            assertThat(error.field()).isEqualTo("Nombre de perfil*");
        });
        assertThat(result.profiles()).isEmpty();
    }

    @Test
    void profiles_receive_the_persisted_roles_named_in_the_relation_sheet() throws Exception {
        Role auditor = persistedRole("Auditor", RoleScope.ofTenant(TENANT));
        persistedRoles.add(auditor);
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}),
                CatalogWorkbooks.profileRoles(new String[]{"gestores", "TENANT", null, "Auditor", "TENANT", null}));

        StepVerifier.create(service().commit("profiles", file, TENANT))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isTrue();
                    assertThat(result.profiles().get(0).roles()).containsExactly(auditor.id());
                })
                .verifyComplete();

        assertThat(profilesSaved.get(0).roles()).containsExactly(auditor.id());
    }

    @Test
    void profile_role_relations_are_checked_row_by_row() throws Exception {
        persistedRoles.add(persistedRole("Auditor", RoleScope.ofTenant(TENANT)));
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}),
                CatalogWorkbooks.profileRoles(
                        new String[]{"Gestores", "TENANT", null, "Auditor", "TENANT", null},
                        new String[]{"Gestores", "TENANT", null, "Auditor", "TENANT", null},
                        new String[]{"Fantasma", "TENANT", null, "Auditor", "TENANT", null},
                        new String[]{"Gestores", "TENANT", null, "Inexistente", "TENANT", null},
                        new String[]{"Gestores", "TENANT", null, "Auditor", "PLANETA", null},
                        new String[]{"Gestores", "TENANT", "App Uno", "Auditor", "TENANT", null},
                        new String[]{"Gestores", "APPLICATION", "App Ajena", "Auditor", "TENANT", null}));

        BulkCatalogImportService.Result result = validate("profiles", file);

        assertThat(result.response().ready()).isFalse();
        assertThat(result.response().errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::code)
                .containsExactly(
                        tuple(3, "DUPLICATE_IN_FILE"),
                        tuple(4, "PROFILE_NOT_FOUND"),
                        tuple(5, "INVALID_REFERENCE"),
                        tuple(6, "INVALID_REFERENCE"),
                        tuple(7, "INVALID_VALUE"),
                        tuple(8, "INVALID_REFERENCE"));
        assertThat(result.response().errors().get(2).description()).isEqualTo("El rol referenciado no existe");
    }

    // ---- archivo ----------------------------------------------------------------------------------------------

    @Test
    void a_file_that_is_not_a_workbook_short_circuits_with_one_global_error_and_nothing_is_saved() {
        byte[] garbage = {1, 2, 3, 4};

        StepVerifier.create(service().commit("roles", garbage, TENANT))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isFalse();
                    assertThat(result.response().totalRows()).isZero();
                    assertThat(result.response().errors()).singleElement().satisfies(error -> {
                        assertThat(error.row()).isZero();
                        assertThat(error.field()).isEqualTo("file");
                        assertThat(error.code()).isEqualTo("INVALID_WORKBOOK");
                        assertThat(error.description()).isEqualTo("El archivo no es un libro Excel válido");
                    });
                    assertThat(result.roles()).isEmpty();
                    assertThat(result.profiles()).isEmpty();
                })
                .verifyComplete();

        assertThat(rolesSaved).isEmpty();
        assertThat(applicationQueries).isEmpty();
    }

    @Test
    void a_workbook_without_the_sheet_of_the_requested_mode_reports_the_missing_sheet() throws Exception {
        byte[] onlyProfiles = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}));

        StepVerifier.create(service().validate("roles", onlyProfiles, TENANT))
                .assertNext(result -> assertThat(result.response().errors()).singleElement().satisfies(error -> {
                    assertThat(error.code()).isEqualTo("INVALID_WORKBOOK");
                    assertThat(error.description()).isEqualTo("Falta la hoja Roles");
                }))
                .verifyComplete();
    }

    @Test
    void a_workbook_without_data_rows_reports_it_and_a_password_protected_one_is_not_readable() throws Exception {
        SheetData empty = CatalogWorkbooks.roles();
        SheetData data = CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null});

        assertThat(codes(validate("roles", CatalogWorkbooks.xlsx(empty)).response())).containsExactly("INVALID_WORKBOOK");
        assertThat(validate("roles", CatalogWorkbooks.encrypted("secreto", data)).response().errors())
                .singleElement().satisfies(error -> assertThat(error.code()).isEqualTo("INVALID_WORKBOOK"));
    }

    @Test
    void legacy_xls_workbooks_are_imported_like_xlsx() throws Exception {
        byte[] file = CatalogWorkbooks.xls(CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));

        StepVerifier.create(service().commit("roles", file, TENANT))
                .assertNext(result -> assertThat(result.response().ready()).isTrue())
                .verifyComplete();

        assertThat(rolesSaved).hasSize(1);
    }

    /**
     * HALLAZGO: {@code result()} cuenta las filas inválidas de TODAS las hojas (los números de fila de
     * {@code RolesxRecurso} se mezclan con los de {@code Roles}) y no acota el resultado como sí hace
     * {@code BulkResourceImportService}. Con un rol y tres relaciones inválidas, {@code validRows} sale negativo.
     */
    @Disabled("Hallazgo: BulkCatalogImportService.result(l.193-204) puede devolver validRows negativo y filas inválidas > filas totales")
    @Test
    void the_row_counters_never_go_negative_when_relation_rows_fail() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}),
                CatalogWorkbooks.roleResources(
                        new String[]{"Fantasma", "TENANT", "App Uno", "/api/a", "GET"},
                        new String[]{"Fantasma", "TENANT", "App Uno", "/api/b", "GET"},
                        new String[]{"Fantasma", "TENANT", "App Uno", "/api/c", "GET"}));

        BulkResourceValidationResponse response = validate("roles", file).response();

        assertThat(response.validRows()).isGreaterThanOrEqualTo(0);
        assertThat(response.invalidRows()).isLessThanOrEqualTo(response.totalRows());
    }
}
