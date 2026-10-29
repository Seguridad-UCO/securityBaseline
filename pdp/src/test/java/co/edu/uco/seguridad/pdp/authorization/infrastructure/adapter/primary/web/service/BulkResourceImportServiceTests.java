package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validación y confirmación de la carga masiva: resuelve la aplicación por nombre dentro del inquilino,
 * exige ser administrador, detecta duplicados persistidos y solo confirma un lote completamente válido.
 */
class BulkResourceImportServiceTests {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final TenantId TENANT = new TenantId("tenant-a");
    private static final UserId ACTOR = new UserId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
    private static final ApplicationId APP_ID = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private static final PdpPrincipal PRINCIPAL = new PdpPrincipal(TENANT, "subject-1", "jti-1",
            Optional.of(ACTOR), AuthenticationContextEvidence.NONE);

    private final BulkResourceWorkbookService workbooks = new BulkResourceWorkbookService();
    private final List<ProtectedResource> committed = new ArrayList<>();
    private final List<ResourcePath> persisted = new ArrayList<>();
    private boolean administrator = true;

    private BulkResourceImportService service() {
        AtomicInteger sequence = new AtomicInteger();
        ProtectedResourceRepository repository = new ProtectedResourceRepository() {
            @Override
            public Mono<Boolean> existsByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path, HttpVerb method) {
                return Mono.just(persisted.contains(path));
            }

            @Override
            public Flux<ProtectedResource> findAllByApplication(ApplicationId applicationId) {
                return Flux.empty();
            }

            @Override
            public Mono<ProtectedResource> save(ProtectedResource resource) {
                return Mono.just(resource);
            }

            @Override
            public Mono<Void> deleteById(ResourceId resourceId) {
                return Mono.empty();
            }

            @Override
            public Mono<ApplicationId> findApplicationIdById(ResourceId resourceId) {
                return Mono.empty();
            }
        };
        return new BulkResourceImportService(workbooks,
                query -> query.name().value().equals("Mi aplicación")
                        ? Mono.just(new RegisteredApplicationResponse(APP_ID, TENANT, query.name(), "",
                        new ApplicationBaseUrl("https://app.example.com"), NOW))
                        : Mono.empty(),
                request -> administrator ? Mono.empty() : Mono.error(new IllegalStateException("denegado")),
                repository,
                () -> new UUID(0, sequence.incrementAndGet()),
                () -> NOW,
                batch -> {
                    committed.addAll(batch);
                    return Mono.empty();
                });
    }

    private static byte[] workbook(String[]... rows) throws Exception {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("Recursos");
            String[] headers = {"Nombre de aplicación", "Ruta", "Método HTTP"};
            var header = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                header.createCell(c).setCellValue(headers[c]);
            }
            for (int r = 0; r < rows.length; r++) {
                var row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            book.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void a_valid_batch_is_ready_and_commit_registers_it_once() throws Exception {
        byte[] file = workbook(new String[]{"Mi aplicación", "/api/orders", "GET"},
                new String[]{"Mi aplicación", "/api/orders", "POST"});

        StepVerifier.create(service().commit(file, PRINCIPAL, ACTOR))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isTrue();
                    assertThat(result.response().totalRows()).isEqualTo(2);
                    assertThat(result.response().validRows()).isEqualTo(2);
                    assertThat(result.resources()).hasSize(2);
                })
                .verifyComplete();

        assertThat(committed).hasSize(2).allSatisfy(resource -> {
            assertThat(resource.applicationId()).isEqualTo(APP_ID);
            assertThat(resource.tenantId()).isEqualTo(TENANT);
        });
    }

    @Test
    void validate_never_commits() throws Exception {
        byte[] file = workbook(new String[]{"Mi aplicación", "/api/orders", "GET"});

        StepVerifier.create(service().validate(file, PRINCIPAL, ACTOR))
                .assertNext(result -> assertThat(result.response().ready()).isTrue())
                .verifyComplete();

        assertThat(committed).isEmpty();
    }

    @Test
    void an_unknown_application_marks_the_row_and_blocks_the_commit() throws Exception {
        byte[] file = workbook(new String[]{"Otra", "/api/orders", "GET"},
                new String[]{"Mi aplicación", "/api/items", "GET"});

        StepVerifier.create(service().commit(file, PRINCIPAL, ACTOR))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isFalse();
                    assertThat(result.response().invalidRows()).isEqualTo(1);
                    assertThat(result.response().errors()).extracting(e -> e.code()).contains("APPLICATION_NOT_FOUND");
                })
                .verifyComplete();

        assertThat(committed).isEmpty();
    }

    @Test
    void a_row_that_already_exists_is_reported_as_a_persisted_duplicate() throws Exception {
        persisted.add(new ResourcePath("/api/orders"));
        byte[] file = workbook(new String[]{"Mi aplicación", "/api/orders", "GET"});

        StepVerifier.create(service().validate(file, PRINCIPAL, ACTOR))
                .assertNext(result -> assertThat(result.response().errors())
                        .extracting(e -> e.code()).contains("DUPLICATE_PERSISTED"))
                .verifyComplete();
    }

    @Test
    void a_principal_that_does_not_administer_the_application_is_reported_not_authorized() throws Exception {
        administrator = false;
        byte[] file = workbook(new String[]{"Mi aplicación", "/api/orders", "GET"});

        StepVerifier.create(service().commit(file, PRINCIPAL, ACTOR))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isFalse();
                    assertThat(result.response().errors()).extracting(e -> e.code()).contains("NOT_AUTHORIZED");
                })
                .verifyComplete();

        assertThat(committed).isEmpty();
    }

    @Test
    void invalid_values_are_reported_per_row() throws Exception {
        byte[] file = workbook(new String[]{"Mi aplicación", "sin-barra", "GET"},
                new String[]{"Mi aplicación", "/api/orders", "FETCH"});

        StepVerifier.create(service().validate(file, PRINCIPAL, ACTOR))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isFalse();
                    assertThat(result.response().errors()).extracting(e -> e.code()).contains("INVALID_VALUE");
                    assertThat(result.response().invalidRows()).isEqualTo(2);
                })
                .verifyComplete();
    }

    @Test
    void a_broken_workbook_short_circuits_with_its_parse_error() {
        StepVerifier.create(service().commit(new byte[]{1, 2, 3}, PRINCIPAL, ACTOR))
                .assertNext(result -> {
                    assertThat(result.response().ready()).isFalse();
                    assertThat(result.response().errors()).extracting(e -> e.code()).contains("INVALID_WORKBOOK");
                })
                .verifyComplete();

        assertThat(committed).isEmpty();
    }
}
