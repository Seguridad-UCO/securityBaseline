package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.SearchProtectedApplicationsRequest;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedApplicationUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.SearchProtectedApplicationsUseCase;
import co.edu.uco.seguridad.pdp.resources.domain.ActionCode;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.ResourceCode;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedApplicationRawRequest;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.SearchProtectedApplicationsRawRequest;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.impl.RegisterProtectedApplicationInteractorImpl;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.impl.SearchProtectedApplicationsInteractorImpl;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import co.edu.uco.seguridad.shared.web.PageResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que el interactor traduce correctamente: raw + principal autenticado → tipado → dominio
 * → respuesta HTTP. Cada ejecución se envuelve con {@link TestJwtSupport#withPrincipal} porque el
 * interactor lee el tenant del principal (ADR-0003), no del payload.
 */
class ProtectedApplicationControllerMappingTests {

    private static final Instant AT = Instant.parse("2026-08-07T12:00:00Z");
    private static final ApplicationId APPLICATION_ID = new ApplicationId(UUID.randomUUID());
    private static final ResourceId RESOURCE_ID = new ResourceId(UUID.randomUUID());
    private static final String TENANT = "universidad-uco";

    @Test
    void register_maps_raw_request_to_typed_dto_and_entry_to_flat_response() {
        AtomicReference<RegisterProtectedApplicationRequest> received = new AtomicReference<>();
        RegisterProtectedApplicationUseCase useCase = dto -> {
            received.set(dto);
            return Mono.just(resource());
        };

        Mono<ProtectedApplicationResponse> result = new RegisterProtectedApplicationInteractorImpl(useCase)
                .execute(new RegisterProtectedApplicationRawRequest("gestion-academica", "estudiantes", "consultar"))
                .contextWrite(TestJwtSupport.withPrincipal(TENANT, "test-subject"));

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.applicationId()).isEqualTo(APPLICATION_ID.value().toString());
                    assertThat(response.resourceId()).isEqualTo(RESOURCE_ID.value().toString());
                    assertThat(response.tenantId()).isEqualTo(TENANT);
                    assertThat(response.resourceCode()).isEqualTo("estudiantes");
                    assertThat(response.registeredAt()).isEqualTo(AT);
                })
                .verifyComplete();

        assertThat(received.get().tenantId()).isEqualTo(new TenantId(TENANT));
        assertThat(received.get().resourceCode()).isEqualTo(new ResourceCode("estudiantes"));
    }

    @Test
    void search_maps_raw_request_and_carries_window_metadata_into_the_response() {
        SearchProtectedApplicationsUseCase useCase = dto ->
                Mono.just(ResultPage.of(List.of(resource()), 12, dto.window()));

        Mono<PageResponse<ProtectedApplicationResponse>> result = new SearchProtectedApplicationsInteractorImpl(useCase)
                .execute(new SearchProtectedApplicationsRawRequest(null, null, "1", "5", null, null))
                .contextWrite(TestJwtSupport.withPrincipal(TENANT, "test-subject"));

        StepVerifier.create(result)
                .assertNext(page -> {
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.total()).isEqualTo(12);
                    assertThat(page.page()).isEqualTo(1);
                    assertThat(page.offset()).isEqualTo(5);
                    assertThat(page.limit()).isEqualTo(5);
                })
                .verifyComplete();
    }

    @Test
    void search_passes_criteria_to_use_case_untouched() {
        AtomicReference<SearchProtectedApplicationsRequest> received = new AtomicReference<>();
        SearchProtectedApplicationsUseCase useCase = dto -> {
            received.set(dto);
            return Mono.just(ResultPage.of(List.of(), 0, dto.window()));
        };

        new SearchProtectedApplicationsInteractorImpl(useCase)
                .execute(new SearchProtectedApplicationsRawRequest("academica", "estud", null, null, null, null))
                .contextWrite(TestJwtSupport.withPrincipal(TENANT, "test-subject"))
                .block();

        assertThat(received.get().criteria().tenantId()).isEqualTo(new TenantId(TENANT));
        assertThat(received.get().criteria().nameContains()).contains("academica");
        assertThat(received.get().criteria().resourceContains()).contains("estud");
        assertThat(received.get().window()).isEqualTo(PageWindow.defaultWindow());
    }

    private static ProtectedResource resource() {
        return ProtectedResource.register(
                RESOURCE_ID,
                APPLICATION_ID,
                new TenantId(TENANT),
                new ApplicationName("gestion-academica"),
                new ResourceCode("estudiantes"),
                new ActionCode("consultar"),
                AT);
    }
}
