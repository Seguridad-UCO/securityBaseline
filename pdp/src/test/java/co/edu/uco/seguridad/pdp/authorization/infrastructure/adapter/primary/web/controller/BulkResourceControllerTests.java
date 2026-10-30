package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationNameForTenantLookupValidator;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustAdministerAtLeastOneApplicationValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.BulkCatalogImportService;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.BulkCatalogWorkbookService;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.BulkResourceImportService;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.BulkResourceWorkbookService;
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
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rutas de carga masiva de catálogo (HU-035): una modalidad no soportada llega al cliente como un campo
 * {@code mode} mal formado ({@code MalformedRequestFieldException}, que {@code ApiErrorHandler} responde 400) y
 * nunca como una falla interna; las rutas entregan al servicio el principal y el usuario ya resueltos.
 * El controlador se construye a mano, con los servicios reales sobre fakes.
 */
class BulkResourceControllerTests {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final String TENANT_VALUE = "tenant-a";
    private static final String SUBJECT = "subject-1";
    private static final UserId ACTOR = new UserId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
    private static final ApplicationId APP_UNO = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));

    private final List<AdministrationRequest> gateCalls = new ArrayList<>();
    private final List<String> resolvedSubjects = new ArrayList<>();

    private BulkResourceController controller() {
        AtomicInteger sequence = new AtomicInteger();
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
                throw new UnsupportedOperationException();
            }
        };
        RoleRepository roles = new RoleRepository() {
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
                throw new UnsupportedOperationException();
            }
        };
        ProfileRepository profiles = new ProfileRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(ProfileName name, RoleScope scope) {
                throw new UnsupportedOperationException();
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
                throw new UnsupportedOperationException();
            }
        };
        ApplicationNameForTenantLookupValidator applications = query -> query.name().value().equals("App Uno")
                ? Mono.just(new RegisteredApplicationResponse(APP_UNO, query.tenantId(), query.name(), "",
                new ApplicationBaseUrl("https://app.example.com"), NOW))
                : Mono.empty();
        // El gate niega siempre: así ninguna ruta necesita tocar un repositorio y se puede ver lo que recibe
        PrincipalMustBeApplicationAdministratorValidator denyAll = request -> {
            gateCalls.add(request);
            return Mono.error(new NotAuthorizedToAdministerException(request.tenantId(), request.applicationId()));
        };
        // HU-036: las rutas de esta clase no cargan filas TENANT; si alguna lo hiciera, el actor de inquilino también se niega
        PrincipalMustAdministerAtLeastOneApplicationValidator tenantDenyAll = actor -> Mono.error(
                new NotAuthorizedToAdministerTenantCatalogException(actor.tenantId(), Optional.empty()));
        BulkCatalogWorkbookService catalogWorkbooks = new BulkCatalogWorkbookService();
        BulkCatalogImportService catalogImports = new BulkCatalogImportService(catalogWorkbooks, roles, profiles,
                resources, applications, denyAll, tenantDenyAll, () -> new UUID(0, sequence.incrementAndGet()), () -> NOW);
        BulkResourceImportService resourceImports = new BulkResourceImportService(new BulkResourceWorkbookService(),
                applications, denyAll, resources, () -> new UUID(0, sequence.incrementAndGet()), () -> NOW,
                batch -> Mono.empty());
        return new BulkResourceController(new BulkResourceWorkbookService(), resourceImports, subject -> {
            resolvedSubjects.add(subject);
            return Mono.just(ACTOR);
        }, catalogWorkbooks, catalogImports);
    }

    private static MockServerWebExchange exchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/resources/bulk/catalog/roles").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }

    private static FilePart file(byte[] bytes) {
        return new FilePart() {
            @Override
            public String filename() {
                return "catalogo.xlsx";
            }

            @Override
            public Mono<Void> transferTo(Path dest) {
                return Mono.error(new UnsupportedOperationException());
            }

            @Override
            public String name() {
                return "file";
            }

            @Override
            public HttpHeaders headers() {
                return new HttpHeaders();
            }

            @Override
            public Flux<DataBuffer> content() {
                return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
            }
        };
    }

    /** Libro de roles con una sola fila de datos: un rol de alcance APPLICATION sobre {@code App Uno}. */
    private static byte[] applicationRoleWorkbook() throws Exception {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("Roles");
            String[] headers = {"Nombre de rol*", "Alcance*", "Aplicación"};
            Row header = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                header.createCell(c).setCellValue(headers[c]);
            }
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("Editor");
            data.createCell(1).setCellValue("APPLICATION");
            data.createCell(2).setCellValue("App Uno");
            book.write(out);
            return out.toByteArray();
        }
    }

    private static <T> StepVerifier.FirstStep<T> authenticated(Mono<T> route) {
        return StepVerifier.create(route.contextWrite(TestJwtSupport.withPrincipal(TENANT_VALUE, SUBJECT)));
    }

    private static void assertIsTheMalformedModeField(Throwable error) {
        assertThat(error).isInstanceOfSatisfying(MalformedRequestFieldException.class,
                malformed -> assertThat(malformed.field()).isEqualTo("mode"));
    }

    // ---- modalidad no soportada -------------------------------------------------------------------------------

    @Test
    void the_template_of_an_unsupported_mode_is_a_malformed_mode_field() {
        StepVerifier.create(controller().catalogTemplate("otra"))
                .expectErrorSatisfies(BulkResourceControllerTests::assertIsTheMalformedModeField)
                .verify();
    }

    @Test
    void the_template_is_still_served_for_every_supported_mode_including_resources_roles() {
        for (String mode : List.of("roles", "profiles", "resources-roles")) {
            StepVerifier.create(controller().catalogTemplate(mode))
                    .assertNext(response -> {
                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                        assertThat(response.getBody()).isNotEmpty();
                    })
                    .verifyComplete();
        }
    }

    @Test
    void validating_with_an_unsupported_mode_is_a_malformed_mode_field() throws Exception {
        byte[] workbook = applicationRoleWorkbook();
        BulkResourceController controller = controller();

        for (String mode : List.of("otra", "resources-roles")) {
            authenticated(controller.validateCatalog(mode, file(workbook), exchange()))
                    .expectErrorSatisfies(BulkResourceControllerTests::assertIsTheMalformedModeField)
                    .verify();
        }
        assertThat(gateCalls).isEmpty();
    }

    @Test
    void committing_with_an_unsupported_mode_is_a_malformed_mode_field() throws Exception {
        byte[] workbook = applicationRoleWorkbook();
        BulkResourceController controller = controller();

        for (String mode : List.of("otra", "resources-roles")) {
            authenticated(controller.commitCatalog(mode, file(workbook), exchange()))
                    .expectErrorSatisfies(BulkResourceControllerTests::assertIsTheMalformedModeField)
                    .verify();
        }
        assertThat(gateCalls).isEmpty();
    }

    @Test
    void the_error_report_with_an_unsupported_mode_is_a_malformed_mode_field() throws Exception {
        byte[] workbook = applicationRoleWorkbook();
        BulkResourceController controller = controller();

        for (String mode : List.of("otra", "resources-roles")) {
            authenticated(controller.catalogErrorReport(mode, file(workbook)))
                    .expectErrorSatisfies(BulkResourceControllerTests::assertIsTheMalformedModeField)
                    .verify();
        }
        assertThat(gateCalls).isEmpty();
    }

    // ---- principal y usuario ----------------------------------------------------------------------------------

    @Test
    void validating_hands_the_principal_and_the_resolved_user_to_the_service() throws Exception {
        authenticated(controller().validateCatalog("roles", file(applicationRoleWorkbook()), exchange()))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody().data().ready()).isFalse();
                    assertThat(response.getBody().data().errors()).singleElement()
                            .satisfies(error -> assertThat(error.code()).isEqualTo("NOT_AUTHORIZED"));
                })
                .verifyComplete();

        assertThat(resolvedSubjects).containsExactly(SUBJECT);
        assertThat(gateCalls).singleElement().satisfies(request -> {
            assertThat(request.tenantId()).isEqualTo(new TenantId(TENANT_VALUE));
            assertThat(request.applicationId()).isEqualTo(APP_UNO);
            assertThat(request.subjectUserId()).isEqualTo(ACTOR);
            assertThat(request.subject()).isEqualTo(SUBJECT);
        });
    }

    @Test
    void committing_hands_the_principal_and_the_resolved_user_to_the_service_and_nothing_is_saved() throws Exception {
        authenticated(controller().commitCatalog("roles", file(applicationRoleWorkbook()), exchange()))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody().data().ready()).isFalse();
                    assertThat(response.getBody().data().errors()).extracting(error -> error.code())
                            .containsExactly("NOT_AUTHORIZED");
                })
                .verifyComplete();

        assertThat(resolvedSubjects).containsExactly(SUBJECT);
        assertThat(gateCalls).singleElement().satisfies(request -> {
            assertThat(request.tenantId()).isEqualTo(new TenantId(TENANT_VALUE));
            assertThat(request.subjectUserId()).isEqualTo(ACTOR);
            assertThat(request.subject()).isEqualTo(SUBJECT);
        });
    }

    @Test
    void the_error_report_hands_the_principal_and_the_resolved_user_to_the_service_and_lists_its_errors() throws Exception {
        authenticated(controller().catalogErrorReport("roles", file(applicationRoleWorkbook())))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    try (Workbook report = WorkbookFactory.create(new ByteArrayInputStream(response.getBody()))) {
                        Sheet errors = report.getSheet("Errores");
                        assertThat(errors.getLastRowNum()).isEqualTo(1);
                        assertThat(errors.getRow(1).getCell(2).getStringCellValue()).isEqualTo("NOT_AUTHORIZED");
                    } catch (Exception unreadable) {
                        throw new AssertionError("el reporte debe ser un libro legible", unreadable);
                    }
                })
                .verifyComplete();

        assertThat(gateCalls).singleElement().satisfies(request -> {
            assertThat(request.subjectUserId()).isEqualTo(ACTOR);
            assertThat(request.subject()).isEqualTo(SUBJECT);
        });
    }

    @Test
    void the_error_report_of_an_unreadable_file_is_a_workbook_with_only_the_errors_sheet() {
        authenticated(controller().catalogErrorReport("roles", file(new byte[]{1, 2, 3})))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    try (Workbook report = WorkbookFactory.create(new ByteArrayInputStream(response.getBody()))) {
                        assertThat(report.getNumberOfSheets()).isEqualTo(1);
                        Sheet errors = report.getSheet("Errores");
                        assertThat(errors.getLastRowNum()).isEqualTo(1);
                        assertThat(errors.getRow(1).getCell(2).getStringCellValue()).isEqualTo("INVALID_WORKBOOK");
                    } catch (Exception unreadable) {
                        throw new AssertionError("el reporte debe ser un libro legible", unreadable);
                    }
                })
                .verifyComplete();

        assertThat(gateCalls).isEmpty();
    }
}
