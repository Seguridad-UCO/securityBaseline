package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationNameForTenantQuery;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.MfaEvidenceRequiredException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
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
import co.edu.uco.seguridad.pdp.commons.model.UserId;
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
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Validación y confirmación de la carga masiva de roles y perfiles: resuelve la aplicación por nombre dentro del
 * inquilino autenticado, exige que quien carga administre la aplicación de cada fila (fail-closed), detecta
 * duplicados en el archivo y ya persistidos, resuelve las relaciones y solo confirma un lote completamente válido.
 */
class BulkCatalogImportServiceTests {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final TenantId TENANT = new TenantId("tenant-a");
    private static final TenantId OTHER_TENANT = new TenantId("tenant-b");
    private static final UserId ACTOR = new UserId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
    private static final PdpPrincipal PRINCIPAL = new PdpPrincipal(TENANT, "subject-1", "jti-1",
            Optional.of(ACTOR), AuthenticationContextEvidence.NONE);
    private static final ApplicationId APP_UNO = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private static final ApplicationId APP_DOS = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a2"));
    private static final ApplicationId APP_OTRO_TENANT = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a9"));
    private static final ResourceId ORDERS_GET = new ResourceId(UUID.fromString("00000000-0000-0000-0000-0000000000b1"));
    private static final ResourceId ORDERS_POST = new ResourceId(UUID.fromString("00000000-0000-0000-0000-0000000000b2"));

    private static final String APPLICATION_NOT_FOUND_MESSAGE = "No se encontró la aplicación en el tenant autenticado";
    private static final String NOT_AUTHORIZED_MESSAGE = "No está autorizado para administrar esta aplicación";

    private final BulkCatalogWorkbookService workbooks = new BulkCatalogWorkbookService();
    private final List<ApplicationNameForTenantQuery> applicationQueries = new ArrayList<>();
    private final List<AdministrationRequest> gateCalls = new ArrayList<>();
    private final List<Role> persistedRoles = new ArrayList<>();
    private final List<Profile> persistedProfiles = new ArrayList<>();
    private final Map<String, ResourceId> knownResources = new HashMap<>();
    private final List<Role> rolesSaved = new ArrayList<>();
    private final List<Profile> profilesSaved = new ArrayList<>();
    private final AtomicInteger idsIssued = new AtomicInteger();
    private final AtomicInteger roleExistenceQueries = new AtomicInteger();
    private final AtomicInteger profileExistenceQueries = new AtomicInteger();
    private Function<AdministrationRequest, Mono<Void>> gate = request -> Mono.empty();
    private RuntimeException saveFailure;

    private BulkCatalogImportService service() {
        knownResources.put(APP_UNO.value() + "|/api/orders|GET", ORDERS_GET);
        knownResources.put(APP_UNO.value() + "|/api/orders|POST", ORDERS_POST);
        RoleRepository roles = new RoleRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(RoleName name, RoleScope scope) {
                roleExistenceQueries.incrementAndGet();
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
                profileExistenceQueries.incrementAndGet();
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
        PrincipalMustBeApplicationAdministratorValidator administrators = request -> Mono.defer(() -> {
            gateCalls.add(request);
            return gate.apply(request);
        });
        return new BulkCatalogImportService(workbooks, roles, profiles, resources,
                query -> {
                    applicationQueries.add(query);
                    return switch (query.name().value()) {
                        case "App Uno" -> Mono.just(application(APP_UNO, query));
                        case "App Dos" -> Mono.just(application(APP_DOS, query));
                        // Existe, pero solo en otro inquilino: desde el inquilino del principal es invisible
                        case "App De Otro Tenant" -> query.tenantId().equals(OTHER_TENANT)
                                ? Mono.just(application(APP_OTRO_TENANT, query)) : Mono.empty();
                        case "App Rota" -> Mono.error(new IllegalStateException("catálogo de aplicaciones caído"));
                        default -> Mono.empty();
                    };
                },
                administrators,
                () -> new UUID(0, idsIssued.incrementAndGet()),
                () -> NOW);
    }

    private static RegisteredApplicationResponse application(ApplicationId id, ApplicationNameForTenantQuery query) {
        return new RegisteredApplicationResponse(id, query.tenantId(), query.name(), "",
                new ApplicationBaseUrl("https://app.example.com"), NOW);
    }

    private static Role persistedRole(String name, RoleScope scope) {
        return Role.define(new RoleId(UUID.fromString("00000000-0000-0000-0000-0000000000c1")), new RoleName(name), scope, NOW);
    }

    private static Mono<Void> denied(AdministrationRequest request) {
        return Mono.error(new NotAuthorizedToAdministerException(request.tenantId(), request.applicationId()));
    }

    private static List<String> codes(BulkResourceValidationResponse response) {
        return response.errors().stream().map(BulkResourceRowError::code).toList();
    }

    private static List<Integer> rows(BulkResourceValidationResponse response) {
        return response.errors().stream().map(BulkResourceRowError::row).toList();
    }

    private static void assertCountersAreConsistent(BulkResourceValidationResponse response) {
        assertThat(response.validRows()).isGreaterThanOrEqualTo(0);
        assertThat(response.invalidRows()).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(response.totalRows());
        assertThat(response.validRows() + response.invalidRows()).isEqualTo(response.totalRows());
    }

    private BulkCatalogImportService.Result validate(String mode, byte[] file) {
        AtomicReference<BulkCatalogImportService.Result> validated = new AtomicReference<>();
        StepVerifier.create(service().validate(mode, file, PRINCIPAL, ACTOR)).assertNext(validated::set).verifyComplete();
        return validated.get();
    }

    private void assertNothingWasTouched() {
        assertThat(applicationQueries).isEmpty();
        assertThat(gateCalls).isEmpty();
        assertThat(idsIssued).hasValue(0);
        assertThat(roleExistenceQueries).hasValue(0);
        assertThat(profileExistenceQueries).hasValue(0);
        assertThat(rolesSaved).isEmpty();
        assertThat(profilesSaved).isEmpty();
    }

    // ---- roles: camino feliz ----------------------------------------------------------------------------------

    @Test
    void a_clean_roles_sheet_is_ready_and_builds_the_roles_in_the_authenticated_tenant() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Auditor", "TENANT", null},
                new String[]{"Editor", "application", "App Uno"}));

        StepVerifier.create(service().validate("roles", file, PRINCIPAL, ACTOR))
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
    void the_tenant_of_the_batch_is_the_one_of_the_principal() throws Exception {
        PdpPrincipal otherTenantPrincipal = new PdpPrincipal(OTHER_TENANT, "subject-2", "jti-2",
                Optional.of(ACTOR), AuthenticationContextEvidence.NONE);
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Auditor", "TENANT", null},
                new String[]{"Editor", "APPLICATION", "App Uno"}));

        StepVerifier.create(service().validate("roles", file, otherTenantPrincipal, ACTOR))
                .assertNext(result -> assertThat(result.roles()).extracting(Role::scope).containsExactly(
                        RoleScope.ofTenant(OTHER_TENANT), RoleScope.ofApplication(OTHER_TENANT, APP_UNO)))
                .verifyComplete();

        assertThat(applicationQueries).extracting(ApplicationNameForTenantQuery::tenantId).containsOnly(OTHER_TENANT);
        assertThat(gateCalls).extracting(AdministrationRequest::tenantId).containsOnly(OTHER_TENANT);
    }

    @Test
    void commit_saves_a_valid_roles_batch_atomically_and_never_touches_profiles() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Auditor", "TENANT", null},
                new String[]{"Editor", "APPLICATION", "App Uno"}));

        StepVerifier.create(service().commit("roles", file, PRINCIPAL, ACTOR))
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

        StepVerifier.create(service().commit("roles", file, PRINCIPAL, ACTOR))
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

        StepVerifier.create(service().validate("roles", file, PRINCIPAL, ACTOR))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isTrue();
                    assertThat(result.roles().get(0).resources()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void in_roles_mode_only_roles_are_built_and_only_their_identifiers_are_consumed() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}, new String[]{"Editor", "TENANT", null}),
                CatalogWorkbooks.profiles(new String[]{"No debe leerse", "TENANT", null}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(result.roles()).hasSize(2);
        assertThat(result.profiles()).isEmpty();
        assertThat(idsIssued).hasValue(2);
    }

    @Test
    void in_profiles_mode_only_profiles_are_built_and_only_their_identifiers_are_consumed() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"No debe leerse", "TENANT", null}),
                CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}, new String[]{"Operadores", "TENANT", null}));

        BulkCatalogImportService.Result result = validate("profiles", file);

        assertThat(result.profiles()).hasSize(2);
        assertThat(result.roles()).isEmpty();
        assertThat(idsIssued).hasValue(2);
    }

    // ---- gate de administrador por fila -----------------------------------------------------------------------

    @Test
    void the_gate_receives_the_tenant_application_user_subject_and_authentication_evidence() throws Exception {
        AuthenticationContextEvidence evidence = new AuthenticationContextEvidence(Optional.of("urn:mfa:otp"), List.of("otp"));
        PdpPrincipal principal = new PdpPrincipal(TENANT, "subject-9", "jti-9", Optional.empty(), evidence);
        UserId resolvedUser = new UserId(UUID.fromString("00000000-0000-0000-0000-0000000000bb"));
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Editor", "APPLICATION", "App Uno"}));

        StepVerifier.create(service().validate("roles", file, principal, resolvedUser))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isTrue();
                    assertThat(result.roles()).hasSize(1);
                })
                .verifyComplete();

        assertThat(gateCalls).containsExactly(
                new AdministrationRequest(TENANT, APP_UNO, resolvedUser, "subject-9", Set.of(), evidence));
    }

    @Test
    void a_principal_that_does_not_administer_the_application_gets_not_authorized_on_the_row_and_nothing_is_created() throws Exception {
        gate = BulkCatalogImportServiceTests::denied;
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Editor", "APPLICATION", "App Uno"}));

        StepVerifier.create(service().commit("roles", file, PRINCIPAL, ACTOR))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isFalse();
                    assertThat(result.response().errors()).singleElement().satisfies(error -> {
                        assertThat(error.row()).isEqualTo(2);
                        assertThat(error.field()).isEqualTo("Aplicación");
                        assertThat(error.code()).isEqualTo("NOT_AUTHORIZED");
                        assertThat(error.description()).isEqualTo(NOT_AUTHORIZED_MESSAGE);
                    });
                    assertThat(result.roles()).isEmpty();
                })
                .verifyComplete();

        assertThat(rolesSaved).isEmpty();
        assertThat(profilesSaved).isEmpty();
    }

    @Test
    void only_the_rows_of_the_application_the_principal_does_not_administer_fail_and_the_batch_is_not_saved() throws Exception {
        gate = request -> request.applicationId().equals(APP_DOS) ? denied(request) : Mono.empty();
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Auditor", "APPLICATION", "App Uno"},
                new String[]{"Lector", "APPLICATION", "App Dos"},
                new String[]{"Gestor", "TENANT", null}));

        StepVerifier.create(service().commit("roles", file, PRINCIPAL, ACTOR))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isFalse();
                    assertThat(result.response().errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::code)
                            .containsExactly(tuple(3, "NOT_AUTHORIZED"));
                    assertThat(result.response().totalRows()).isEqualTo(3);
                    assertThat(result.response().validRows()).isEqualTo(2);
                    assertThat(result.response().invalidRows()).isEqualTo(1);
                    assertThat(result.roles()).extracting(r -> r.name().value()).containsExactly("Auditor", "Gestor");
                })
                .verifyComplete();

        assertThat(rolesSaved).isEmpty();
    }

    @Test
    void an_unexpected_gate_failure_is_reported_as_not_authorized_without_leaking_its_detail() throws Exception {
        gate = request -> Mono.error(new IllegalStateException("opa caído"));
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Editor", "APPLICATION", "App Uno"}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(result.response().ready()).isFalse();
        assertThat(result.response().errors()).singleElement().satisfies(error -> {
            assertThat(error.code()).isEqualTo("NOT_AUTHORIZED");
            assertThat(error.field()).isEqualTo("Aplicación");
            assertThat(error.description()).isEqualTo(NOT_AUTHORIZED_MESSAGE);
        });
        assertThat(result.roles()).isEmpty();
    }

    @Test
    void a_gate_that_requires_mfa_is_also_reported_as_not_authorized_and_blocks_the_commit() throws Exception {
        gate = request -> Mono.error(new MfaEvidenceRequiredException(request.tenantId(), request.applicationId()));
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(new String[]{"Operadores", "APPLICATION", "App Dos"}));

        StepVerifier.create(service().commit("profiles", file, PRINCIPAL, ACTOR))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isFalse();
                    assertThat(codes(result.response())).containsExactly("NOT_AUTHORIZED");
                    assertThat(result.profiles()).isEmpty();
                })
                .verifyComplete();

        assertThat(profilesSaved).isEmpty();
        assertThat(rolesSaved).isEmpty();
    }

    @Test
    void the_gate_is_called_once_per_application_however_many_rows_point_to_it() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Auditor", "APPLICATION", "App Uno"},
                new String[]{"Editor", "APPLICATION", "App Uno"},
                new String[]{"Lector", "APPLICATION", "App Uno"}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(result.response().ready()).isTrue();
        assertThat(result.roles()).hasSize(3);
        assertThat(gateCalls).hasSize(1);
        assertThat(gateCalls.get(0).applicationId()).isEqualTo(APP_UNO);
    }

    @Test
    void the_gate_is_called_once_for_each_distinct_application() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(
                new String[]{"Gestores", "APPLICATION", "App Uno"},
                new String[]{"Operadores", "APPLICATION", "App Dos"},
                new String[]{"Lectores", "APPLICATION", "App Uno"},
                new String[]{"Analistas", "APPLICATION", "App Dos"}));

        BulkCatalogImportService.Result result = validate("profiles", file);

        assertThat(result.response().ready()).isTrue();
        assertThat(gateCalls).extracting(AdministrationRequest::applicationId).containsExactlyInAnyOrder(APP_UNO, APP_DOS);
    }

    @Test
    void a_denial_is_reused_for_the_other_rows_of_the_application_instead_of_asking_again() throws Exception {
        gate = BulkCatalogImportServiceTests::denied;
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Auditor", "APPLICATION", "App Uno"},
                new String[]{"Editor", "APPLICATION", "App Uno"},
                new String[]{"Lector", "APPLICATION", "App Uno"}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(rows(result.response())).containsExactly(2, 3, 4);
        assertThat(codes(result.response())).containsOnly("NOT_AUTHORIZED");
        assertThat(result.response().invalidRows()).isEqualTo(3);
        assertThat(gateCalls).hasSize(1);
    }

    @Test
    void the_gate_memory_belongs_to_one_call_so_each_call_asks_again() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Editor", "APPLICATION", "App Uno"}));
        BulkCatalogImportService service = service();

        StepVerifier.create(service.validate("roles", file, PRINCIPAL, ACTOR)).expectNextCount(1).verifyComplete();
        StepVerifier.create(service.commit("roles", file, PRINCIPAL, ACTOR)).expectNextCount(1).verifyComplete();

        assertThat(gateCalls).hasSize(2);
    }

    @Test
    void relation_rows_do_not_call_the_gate_again() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Editor", "APPLICATION", "App Uno"}),
                CatalogWorkbooks.roleResources(
                        new String[]{"Editor", "APPLICATION", "App Uno", "/api/orders", "GET"},
                        new String[]{"Editor", "APPLICATION", "App Uno", "/api/orders", "POST"}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(result.response().ready()).isTrue();
        assertThat(gateCalls).hasSize(1);
    }

    @Test
    void the_relations_of_a_role_that_failed_the_gate_are_reported_as_role_not_found() throws Exception {
        gate = BulkCatalogImportServiceTests::denied;
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Editor", "APPLICATION", "App Uno"}),
                CatalogWorkbooks.roleResources(new String[]{"Editor", "APPLICATION", "App Uno", "/api/orders", "GET"}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(result.response().errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::code)
                .containsExactly(tuple(2, "NOT_AUTHORIZED"), tuple(2, "ROLE_NOT_FOUND"));
        assertThat(gateCalls).hasSize(1);
        assertThat(result.roles()).isEmpty();
    }

    @Test
    void the_relations_of_a_profile_that_failed_the_gate_are_reported_as_profile_not_found() throws Exception {
        gate = BulkCatalogImportServiceTests::denied;
        persistedRoles.add(persistedRole("Auditor", RoleScope.ofTenant(TENANT)));
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.profiles(new String[]{"Operadores", "APPLICATION", "App Dos"}),
                CatalogWorkbooks.profileRoles(new String[]{"Operadores", "APPLICATION", "App Dos", "Auditor", "TENANT", null}));

        BulkCatalogImportService.Result result = validate("profiles", file);

        assertThat(result.response().errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::code)
                .containsExactly(tuple(2, "NOT_AUTHORIZED"), tuple(2, "PROFILE_NOT_FOUND"));
        assertThat(gateCalls).hasSize(1);
        assertThat(result.profiles()).isEmpty();
    }

    @Test
    void tenant_scope_rows_do_not_call_the_gate() throws Exception {
        gate = BulkCatalogImportServiceTests::denied;
        byte[] roles = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));
        byte[] profiles = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}));

        assertThat(validate("roles", roles).response().ready()).isTrue();
        assertThat(validate("profiles", profiles).response().ready()).isTrue();

        assertThat(gateCalls).isEmpty();
    }

    @Test
    void an_application_of_another_tenant_is_reported_exactly_like_one_that_does_not_exist_and_the_gate_is_not_asked() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Editor", "APPLICATION", "App De Otro Tenant"},
                new String[]{"Lector", "APPLICATION", "App Inexistente"}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(result.response().errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::field,
                        BulkResourceRowError::code, BulkResourceRowError::description)
                .containsExactly(
                        tuple(2, "Aplicación", "APPLICATION_NOT_FOUND", APPLICATION_NOT_FOUND_MESSAGE),
                        tuple(3, "Aplicación", "APPLICATION_NOT_FOUND", APPLICATION_NOT_FOUND_MESSAGE));
        assertThat(applicationQueries).extracting(ApplicationNameForTenantQuery::tenantId).containsOnly(TENANT);
        assertThat(gateCalls).isEmpty();
        assertThat(result.roles()).isEmpty();
    }

    @Test
    void the_gate_comes_before_the_duplicate_checks_so_a_non_administrator_cannot_probe_for_existing_roles() throws Exception {
        gate = BulkCatalogImportServiceTests::denied;
        persistedRoles.add(persistedRole("Editor", RoleScope.ofApplication(TENANT, APP_UNO)));
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Editor", "APPLICATION", "App Uno"},
                new String[]{"Editor", "APPLICATION", "App Uno"},
                new String[]{"Nuevo", "APPLICATION", "App Uno"}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(rows(result.response())).containsExactly(2, 3, 4);
        assertThat(codes(result.response())).containsOnly("NOT_AUTHORIZED");
        assertThat(roleExistenceQueries).hasValue(0);
    }

    @Test
    void the_gate_comes_before_the_duplicate_checks_for_profiles_too() throws Exception {
        gate = BulkCatalogImportServiceTests::denied;
        persistedProfiles.add(Profile.define(new ProfileId(UUID.fromString("00000000-0000-0000-0000-0000000000d1")),
                new ProfileName("Gestores"), RoleScope.ofApplication(TENANT, APP_DOS), NOW));
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(new String[]{"Gestores", "APPLICATION", "App Dos"}));

        BulkCatalogImportService.Result result = validate("profiles", file);

        assertThat(codes(result.response())).containsExactly("NOT_AUTHORIZED");
        assertThat(profileExistenceQueries).hasValue(0);
    }

    @Test
    void an_administrator_still_sees_the_duplicates_of_its_application() throws Exception {
        persistedRoles.add(persistedRole("Editor", RoleScope.ofApplication(TENANT, APP_UNO)));
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Editor", "APPLICATION", "App Uno"},
                new String[]{"Lector", "APPLICATION", "App Uno"},
                new String[]{"lector", "APPLICATION", "App Uno"}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(result.response().errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::code)
                .containsExactly(tuple(2, "DUPLICATE_PERSISTED"), tuple(4, "DUPLICATE_IN_FILE"));
        assertThat(gateCalls).hasSize(1);
    }

    @Test
    void a_row_with_an_invalid_name_or_scope_never_reaches_the_application_lookup_or_the_gate() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Ab", "APPLICATION", "App Uno"},
                new String[]{"Gestor", "PLANETA", "App Uno"}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(codes(result.response())).containsExactly("INVALID_VALUE", "INVALID_VALUE");
        assertThat(applicationQueries).isEmpty();
        assertThat(gateCalls).isEmpty();
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
    void an_application_that_does_not_belong_to_the_tenant_is_rejected_with_a_neutral_message() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Editor", "APPLICATION", "App Ajena"}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(result.response().errors()).singleElement().satisfies(error -> {
            assertThat(error.code()).isEqualTo("APPLICATION_NOT_FOUND");
            assertThat(error.field()).isEqualTo("Aplicación");
            assertThat(error.description()).isEqualTo(APPLICATION_NOT_FOUND_MESSAGE);
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
        assertThat(gateCalls).isEmpty();
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
        assertThat(codes(result.response())).containsExactly(
                "INVALID_VALUE", "INVALID_VALUE", "INVALID_VALUE", "INVALID_VALUE", "REQUIRED");
        assertThat(result.response().errors().get(0).description()).isEqualTo("Aplicación solo aplica al alcance APPLICATION");
        assertThat(result.response().errors().get(1).description()).isEqualTo("APPLICATION requiere Aplicación");
        assertThat(result.response().totalRows()).isEqualTo(6);
        assertThat(result.response().validRows()).isEqualTo(1);
        assertThat(result.response().invalidRows()).isEqualTo(5);
        assertThat(result.roles()).extracting(r -> r.name().value()).containsExactly("Valido");
    }

    @Test
    void an_invalid_name_is_reported_under_the_name_column_not_under_the_application() throws Exception {
        byte[] roles = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Ab", "TENANT", null}));
        byte[] profiles = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(new String[]{"Ab", "TENANT", null}));

        assertThat(validate("roles", roles).response().errors()).singleElement().satisfies(error -> {
            assertThat(error.row()).isEqualTo(2);
            assertThat(error.code()).isEqualTo("INVALID_VALUE");
            assertThat(error.field()).isEqualTo("Nombre de rol*");
        });
        assertThat(validate("profiles", profiles).response().errors()).singleElement().satisfies(error -> {
            assertThat(error.code()).isEqualTo("INVALID_VALUE");
            assertThat(error.field()).isEqualTo("Nombre de perfil*");
        });
    }

    @Test
    void an_unknown_scope_and_an_incoherent_application_are_reported_under_their_own_columns() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Gestor", "PLANETA", null},
                new String[]{"Auditor", "TENANT", "App Uno"},
                new String[]{"Editor", "APPLICATION", null}));

        assertThat(validate("roles", file).response().errors())
                .extracting(BulkResourceRowError::row, BulkResourceRowError::field, BulkResourceRowError::code)
                .containsExactly(
                        tuple(2, "Alcance*", "INVALID_VALUE"),
                        tuple(3, "Aplicación", "INVALID_VALUE"),
                        tuple(4, "Aplicación", "INVALID_VALUE"));
    }

    @Test
    void empty_required_cells_are_reported_as_required_and_the_row_is_not_evaluated_any_further() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{null, "TENANT", null},
                new String[]{"Auditor", null, null},
                new String[]{"Lector", null, "App Uno"}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(result.response().errors())
                .extracting(BulkResourceRowError::row, BulkResourceRowError::field, BulkResourceRowError::code,
                        BulkResourceRowError::description)
                .containsExactly(
                        tuple(2, "Nombre de rol*", "REQUIRED", "El nombre del rol es obligatorio"),
                        tuple(3, "Alcance*", "REQUIRED", "El alcance es obligatorio"),
                        tuple(4, "Alcance*", "REQUIRED", "El alcance es obligatorio"));
        assertThat(applicationQueries).isEmpty();
        assertThat(gateCalls).isEmpty();
        assertThat(roleExistenceQueries).hasValue(0);
    }

    @Test
    void an_empty_profile_name_is_required_under_the_profile_column() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(new String[]{null, "TENANT", null}));

        assertThat(validate("profiles", file).response().errors()).singleElement().satisfies(error -> {
            assertThat(error.row()).isEqualTo(2);
            assertThat(error.field()).isEqualTo("Nombre de perfil*");
            assertThat(error.code()).isEqualTo("REQUIRED");
            assertThat(error.description()).isEqualTo("El nombre del perfil es obligatorio");
        });
    }

    @Test
    void commit_does_not_save_anything_when_a_single_row_is_invalid() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"Auditor", "TENANT", null},
                new String[]{"Editor", "APPLICATION", "App Ajena"}));

        StepVerifier.create(service().commit("roles", file, PRINCIPAL, ACTOR))
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

        StepVerifier.create(service().commit("roles", file, PRINCIPAL, ACTOR))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(IllegalStateException.class).hasMessage("conflicto de unicidad"))
                .verify();

        assertThat(rolesSaved).isEmpty();
    }

    // ---- celdas ilegibles -------------------------------------------------------------------------------------

    @Test
    void an_unreadable_cell_is_an_error_of_its_row_which_counts_as_invalid_and_is_not_evaluated_again() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new Object[]{12345, "TENANT", null},
                new Object[]{"Auditor", new CatalogWorkbooks.Formula("1+1"), null},
                new String[]{"Editor", "TENANT", null}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(result.response().errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::field, BulkResourceRowError::code)
                .containsExactly(
                        tuple(2, "Nombre de rol*", "UNEXPECTED_CELL_TYPE"),
                        tuple(3, "Alcance*", "UNSUPPORTED_CELL"));
        assertThat(result.response().ready()).isFalse();
        assertThat(result.response().totalRows()).isEqualTo(3);
        assertThat(result.response().validRows()).isEqualTo(1);
        assertThat(result.response().invalidRows()).isEqualTo(2);
        assertThat(result.roles()).extracting(r -> r.name().value()).containsExactly("Editor");
    }

    @Test
    void an_unreadable_cell_in_a_relation_sheet_blocks_the_commit_without_changing_the_counters() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}),
                CatalogWorkbooks.roleResources(new Object[]{"Auditor", "TENANT", "App Uno", 404, "GET"}));

        StepVerifier.create(service().commit("roles", file, PRINCIPAL, ACTOR))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isFalse();
                    assertThat(result.response().errors()).extracting(BulkResourceRowError::row,
                                    BulkResourceRowError::field, BulkResourceRowError::code)
                            .containsExactly(tuple(2, "RolesxRecurso", "UNEXPECTED_CELL_TYPE"));
                    assertThat(result.response().totalRows()).isEqualTo(1);
                    assertThat(result.response().validRows()).isEqualTo(1);
                    assertThat(result.response().invalidRows()).isZero();
                })
                .verifyComplete();

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
                        tuple(7, "REQUIRED"),
                        tuple(8, "INVALID_REFERENCE"));
        assertThat(result.response().errors().get(2).description()).isEqualTo(APPLICATION_NOT_FOUND_MESSAGE);
        assertThat(result.response().errors().get(3).description()).isEqualTo("El recurso no existe en la aplicación indicada");
        assertThat(gateCalls).isEmpty();
    }

    @Test
    void every_empty_required_cell_of_a_role_resource_relation_is_required() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}),
                CatalogWorkbooks.roleResources(
                        new String[]{"Auditor", "TENANT", "App Uno", "/api/orders", "GET"},
                        new String[]{"", "TENANT", "App Uno", "/api/orders", "GET"},
                        new String[]{"Auditor", "", "App Uno", "/api/orders", "GET"},
                        new String[]{"Auditor", "TENANT", "", "/api/orders", "GET"},
                        new String[]{"Auditor", "TENANT", "App Uno", "", "GET"},
                        new String[]{"Auditor", "TENANT", "App Uno", "/api/orders", ""}));

        BulkCatalogImportService.Result result = validate("roles", file);

        assertThat(result.response().errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::code)
                .containsExactly(tuple(3, "REQUIRED"), tuple(4, "REQUIRED"), tuple(5, "REQUIRED"),
                        tuple(6, "REQUIRED"), tuple(7, "REQUIRED"));
    }

    @Test
    void a_relation_row_that_cannot_be_read_blocks_the_commit() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}),
                CatalogWorkbooks.roleResources(new String[]{"Auditor", "TENANT", "App Uno", "/api/inexistente", "GET"}));

        StepVerifier.create(service().commit("roles", file, PRINCIPAL, ACTOR))
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

        StepVerifier.create(service().commit("profiles", file, PRINCIPAL, ACTOR))
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
    void a_profile_batch_of_an_application_the_principal_does_not_administer_is_not_saved() throws Exception {
        gate = BulkCatalogImportServiceTests::denied;
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(
                new String[]{"Gestores", "TENANT", null},
                new String[]{"Operadores", "APPLICATION", "App Dos"}));

        StepVerifier.create(service().commit("profiles", file, PRINCIPAL, ACTOR))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isFalse();
                    assertThat(result.response().errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::field,
                                    BulkResourceRowError::code)
                            .containsExactly(tuple(3, "Aplicación", "NOT_AUTHORIZED"));
                })
                .verifyComplete();

        assertThat(profilesSaved).isEmpty();
        assertThat(rolesSaved).isEmpty();
    }

    @Test
    void profiles_receive_the_persisted_roles_named_in_the_relation_sheet() throws Exception {
        Role auditor = persistedRole("Auditor", RoleScope.ofTenant(TENANT));
        persistedRoles.add(auditor);
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}),
                CatalogWorkbooks.profileRoles(new String[]{"gestores", "TENANT", null, "Auditor", "TENANT", null}));

        StepVerifier.create(service().commit("profiles", file, PRINCIPAL, ACTOR))
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
        assertThat(result.response().errors().get(5).description()).isEqualTo(APPLICATION_NOT_FOUND_MESSAGE);
        assertThat(gateCalls).isEmpty();
    }

    @Test
    void every_empty_required_cell_of_a_profile_role_relation_is_required() throws Exception {
        persistedRoles.add(persistedRole("Auditor", RoleScope.ofTenant(TENANT)));
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}),
                CatalogWorkbooks.profileRoles(
                        new String[]{"Gestores", "TENANT", null, "Auditor", "TENANT", null},
                        new String[]{"", "TENANT", null, "Auditor", "TENANT", null},
                        new String[]{"Gestores", "", null, "Auditor", "TENANT", null},
                        new String[]{"Gestores", "TENANT", null, "", "TENANT", null},
                        new String[]{"Gestores", "TENANT", null, "Auditor", "", null}));

        BulkCatalogImportService.Result result = validate("profiles", file);

        assertThat(result.response().errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::code)
                .containsExactly(tuple(3, "REQUIRED"), tuple(4, "REQUIRED"), tuple(5, "REQUIRED"), tuple(6, "REQUIRED"));
    }

    // ---- modalidad --------------------------------------------------------------------------------------------

    @Test
    void an_unsupported_mode_is_rejected_by_validate_and_commit_before_touching_anything() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Editor", "APPLICATION", "App Uno"}),
                CatalogWorkbooks.profiles(new String[]{"Operadores", "APPLICATION", "App Dos"}));
        BulkCatalogImportService service = service();

        for (String mode : List.of("otra", "resources-roles", "Roles", "PROFILES")) {
            StepVerifier.create(service.validate(mode, file, PRINCIPAL, ACTOR))
                    .expectError(UnsupportedBulkModeException.class).verify();
            StepVerifier.create(service.commit(mode, file, PRINCIPAL, ACTOR))
                    .expectError(UnsupportedBulkModeException.class).verify();
        }

        assertNothingWasTouched();
    }

    @Test
    void the_mode_is_checked_before_the_file_is_opened() {
        StepVerifier.create(service().validate("otra", new byte[]{1, 2, 3}, PRINCIPAL, ACTOR))
                .expectError(UnsupportedBulkModeException.class).verify();
        StepVerifier.create(service().commit("resources-roles", new byte[0], PRINCIPAL, ACTOR))
                .expectError(UnsupportedBulkModeException.class).verify();

        assertNothingWasTouched();
    }

    // ---- contadores -------------------------------------------------------------------------------------------

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
        assertThat(response.validRows()).isEqualTo(1);
        assertThat(response.invalidRows()).isZero();
        assertThat(response.ready()).isFalse();
        assertThat(response.errors()).hasSize(3);
    }

    @Test
    void relation_errors_block_the_batch_but_never_count_as_invalid_roles_or_profiles() throws Exception {
        byte[] roles = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}, new String[]{"Editor", "TENANT", null}),
                CatalogWorkbooks.roleResources(
                        new String[]{"Fantasma", "TENANT", "App Uno", "/api/a", "GET"},
                        new String[]{"Auditor", "TENANT", "App Ajena", "/api/a", "GET"},
                        new String[]{"Auditor", "TENANT", "App Uno", "/api/inexistente", "GET"},
                        new String[]{"Editor", "TENANT", "", "/api/a", "GET"},
                        new String[]{"Editor", "TENANT", "App Uno", "sin-barra", "GET"}));
        byte[] profiles = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}),
                CatalogWorkbooks.profileRoles(
                        new String[]{"Gestores", "TENANT", null, "Inexistente", "TENANT", null},
                        new String[]{"Gestores", "TENANT", null, "Otro rol", "TENANT", null}));

        BulkResourceValidationResponse rolesResponse = validate("roles", roles).response();
        BulkResourceValidationResponse profilesResponse = validate("profiles", profiles).response();

        assertThat(rolesResponse.ready()).isFalse();
        assertThat(rolesResponse.errors()).hasSize(5);
        assertThat(rolesResponse.totalRows()).isEqualTo(2);
        assertThat(rolesResponse.validRows()).isEqualTo(2);
        assertThat(rolesResponse.invalidRows()).isZero();
        assertThat(profilesResponse.ready()).isFalse();
        assertThat(profilesResponse.errors()).hasSize(2);
        assertThat(profilesResponse.totalRows()).isEqualTo(1);
        assertThat(profilesResponse.validRows()).isEqualTo(1);
        assertThat(profilesResponse.invalidRows()).isZero();
    }

    @Test
    void valid_plus_invalid_always_equals_total_when_main_sheet_and_relation_errors_are_mixed() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(
                        new String[]{"Auditor", "TENANT", null},
                        new String[]{"Ab", "TENANT", null},
                        new String[]{"Editor", "APPLICATION", "App Ajena"}),
                CatalogWorkbooks.roleResources(
                        new String[]{"Fantasma", "TENANT", "App Uno", "/api/a", "GET"},
                        new String[]{"Auditor", "TENANT", "App Uno", "/api/inexistente", "GET"}));

        BulkResourceValidationResponse response = validate("roles", file).response();

        assertThat(response.ready()).isFalse();
        assertThat(response.errors()).hasSize(4);
        assertThat(response.totalRows()).isEqualTo(3);
        assertThat(response.invalidRows()).isEqualTo(2);
        assertThat(response.validRows()).isEqualTo(1);
        assertCountersAreConsistent(response);
    }

    @Test
    void a_row_with_several_errors_counts_as_one_invalid_row() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{null, null, "App Uno"}));

        BulkResourceValidationResponse response = validate("roles", file).response();

        assertThat(response.errors()).isNotEmpty();
        assertThat(response.errors()).extracting(BulkResourceRowError::row).containsOnly(2);
        assertThat(codes(response)).containsOnly("REQUIRED");
        assertThat(response.totalRows()).isEqualTo(1);
        assertThat(response.invalidRows()).isEqualTo(1);
        assertThat(response.validRows()).isZero();
        assertCountersAreConsistent(response);
    }

    @Test
    void profile_counters_follow_the_same_contract() throws Exception {
        persistedRoles.add(persistedRole("Auditor", RoleScope.ofTenant(TENANT)));
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.profiles(
                        new String[]{"Gestores", "TENANT", null},
                        new String[]{"Operadores", "APPLICATION", "App Ajena"}),
                CatalogWorkbooks.profileRoles(
                        new String[]{"Gestores", "TENANT", null, "Inexistente", "TENANT", null},
                        new String[]{"Gestores", "TENANT", null, "Otro rol", "TENANT", null},
                        new String[]{"Gestores", "TENANT", null, "Tercero", "TENANT", null}));

        BulkResourceValidationResponse response = validate("profiles", file).response();

        assertThat(response.ready()).isFalse();
        assertThat(response.totalRows()).isEqualTo(2);
        assertThat(response.invalidRows()).isEqualTo(1);
        assertThat(response.validRows()).isEqualTo(1);
        assertCountersAreConsistent(response);
    }

    @Test
    void a_global_error_of_the_file_blocks_the_batch_without_counting_as_an_invalid_row() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                CatalogWorkbooks.tenantRoleRows("Rol", BulkCatalogWorkbookService.MAX_ROWS + 1)));

        StepVerifier.create(service().commit("roles", file, PRINCIPAL, ACTOR))
                .assertNext(result -> {
                    BulkResourceValidationResponse response = result.response();
                    assertThat(response.ready()).isFalse();
                    assertThat(response.errors()).singleElement().satisfies(error -> {
                        assertThat(error.row()).isZero();
                        assertThat(error.field()).isEqualTo("Roles");
                        assertThat(error.code()).isEqualTo("TOO_MANY_ROWS");
                    });
                    assertThat(response.totalRows()).isEqualTo(BulkCatalogWorkbookService.MAX_ROWS);
                    assertThat(response.invalidRows()).isZero();
                    assertThat(response.validRows()).isEqualTo(BulkCatalogWorkbookService.MAX_ROWS);
                    assertCountersAreConsistent(response);
                })
                .verifyComplete();

        assertThat(rolesSaved).isEmpty();
    }

    // ---- archivo ----------------------------------------------------------------------------------------------

    @Test
    void a_file_that_is_not_a_workbook_short_circuits_with_one_global_error_and_nothing_is_saved() {
        byte[] garbage = {1, 2, 3, 4};

        StepVerifier.create(service().commit("roles", garbage, PRINCIPAL, ACTOR))
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

        assertNothingWasTouched();
    }

    @Test
    void an_empty_or_oversized_file_short_circuits_with_one_global_error() {
        byte[] oversized = new byte[BulkCatalogWorkbookService.MAX_BYTES + 1];

        StepVerifier.create(service().validate("roles", new byte[0], PRINCIPAL, ACTOR))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isFalse();
                    assertThat(result.response().totalRows()).isZero();
                    assertThat(result.response().errors()).singleElement().satisfies(error -> {
                        assertThat(error.row()).isZero();
                        assertThat(error.code()).isEqualTo("EMPTY_FILE");
                        assertThat(error.description()).isEqualTo("El archivo está vacío");
                    });
                })
                .verifyComplete();
        StepVerifier.create(service().commit("profiles", oversized, PRINCIPAL, ACTOR))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isFalse();
                    assertThat(codes(result.response())).containsExactly("FILE_TOO_LARGE");
                })
                .verifyComplete();

        assertNothingWasTouched();
    }

    @Test
    void a_workbook_without_the_sheet_of_the_requested_mode_reports_the_missing_sheet() throws Exception {
        byte[] onlyProfiles = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}));

        StepVerifier.create(service().validate("roles", onlyProfiles, PRINCIPAL, ACTOR))
                .assertNext(result -> assertThat(result.response().errors()).singleElement().satisfies(error -> {
                    assertThat(error.code()).isEqualTo("MISSING_SHEET");
                    assertThat(error.description()).isEqualTo("Falta la hoja Roles");
                }))
                .verifyComplete();
    }

    @Test
    void a_workbook_without_data_rows_reports_it_and_a_password_protected_one_is_reported_as_encrypted() throws Exception {
        SheetData empty = CatalogWorkbooks.roles();
        SheetData data = CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null});

        assertThat(codes(validate("roles", CatalogWorkbooks.xlsx(empty)).response())).containsExactly("EMPTY_FILE");
        assertThat(validate("roles", CatalogWorkbooks.encrypted("secreto", data)).response().errors())
                .singleElement().satisfies(error -> assertThat(error.code()).isEqualTo("ENCRYPTED_FILE"));
    }

    @Test
    void a_changed_header_in_the_main_sheet_short_circuits_with_invalid_headers_and_nothing_is_queried() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.sheet("Roles",
                new String[]{"Nombre", "Alcance*", "Aplicación"}, new String[]{"Editor", "APPLICATION", "App Uno"}));

        StepVerifier.create(service().commit("roles", file, PRINCIPAL, ACTOR))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isFalse();
                    assertThat(result.response().totalRows()).isZero();
                    assertThat(result.response().errors()).extracting(BulkResourceRowError::row,
                                    BulkResourceRowError::field, BulkResourceRowError::code)
                            .containsExactly(tuple(1, "Roles", "INVALID_HEADERS"));
                })
                .verifyComplete();

        assertNothingWasTouched();
    }

    @Test
    void a_changed_header_in_a_relation_sheet_is_an_error_of_that_sheet_and_the_main_sheet_is_still_evaluated() throws Exception {
        byte[] roles = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}),
                CatalogWorkbooks.sheet("RolesxRecurso", new String[]{"Rol", "Alcance*", "Aplicación*", "Ruta*", "Método HTTP*"},
                        new String[]{"Auditor", "TENANT", "App Uno", "/api/orders", "GET"}));
        byte[] profiles = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}),
                CatalogWorkbooks.sheet("PerfilesxRol", new String[]{"Perfil", "Alcance del perfil*"},
                        new String[]{"Gestores", "TENANT"}));

        BulkResourceValidationResponse rolesResponse = validate("roles", roles).response();
        BulkResourceValidationResponse profilesResponse = validate("profiles", profiles).response();

        assertThat(rolesResponse.ready()).isFalse();
        assertThat(rolesResponse.errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::field,
                BulkResourceRowError::code).containsExactly(tuple(1, "RolesxRecurso", "INVALID_HEADERS"));
        assertThat(rolesResponse.totalRows()).isEqualTo(1);
        assertThat(rolesResponse.validRows()).isEqualTo(1);
        assertThat(rolesResponse.invalidRows()).isZero();
        assertThat(profilesResponse.ready()).isFalse();
        assertThat(profilesResponse.errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::field,
                BulkResourceRowError::code).containsExactly(tuple(1, "PerfilesxRol", "INVALID_HEADERS"));
        assertThat(profilesResponse.totalRows()).isEqualTo(1);
        assertThat(profilesResponse.validRows()).isEqualTo(1);
    }

    @Test
    void legacy_xls_workbooks_are_imported_like_xlsx() throws Exception {
        byte[] file = CatalogWorkbooks.xls(CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));

        StepVerifier.create(service().commit("roles", file, PRINCIPAL, ACTOR))
                .assertNext(result -> assertThat(result.response().ready()).isTrue())
                .verifyComplete();

        assertThat(rolesSaved).hasSize(1);
    }
}
