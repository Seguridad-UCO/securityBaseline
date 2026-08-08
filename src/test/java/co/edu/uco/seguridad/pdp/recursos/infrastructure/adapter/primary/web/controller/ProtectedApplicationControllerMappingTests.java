package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.SearchProtectedApplicationsRequest;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.RegisterProtectedApplicationInteractor;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.SearchProtectedApplicationsInteractor;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedApplicationRawRequest;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.SearchProtectedApplicationsRawRequest;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper.ProtectedApplicationResponseMapper;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper.RegisterProtectedApplicationRequestMapper;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper.SearchProtectedApplicationsRequestMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que el adaptador primario web traduce correctamente en ambas direcciones:
 * solicitud HTTP cruda → DTO tipado de aplicación (vía mapper + interactor), y resultado → DTO HTTP.
 */
class ProtectedApplicationControllerMappingTests {

    private static final Instant AT = Instant.parse("2026-08-07T12:00:00Z");
    private static final ApplicationId APPLICATION_ID = new ApplicationId(UUID.randomUUID());
    private static final ResourceId RESOURCE_ID = new ResourceId(UUID.randomUUID());

    @Test
    void register_maps_raw_request_to_typed_dto_and_entry_to_flat_response() {
        AtomicReference<RegisterProtectedApplicationRequest> received = new AtomicReference<>();
        RegisterProtectedApplicationInteractor interactor = dto -> {
            received.set(dto);
            return Mono.just(entry());
        };

        Mono<ProtectedApplicationResponse> result = Mono.fromSupplier(() ->
                        RegisterProtectedApplicationRequestMapper.toRequest(
                                RegisterProtectedApplicationRequestMapper.toValidatedRequest(
                                        new RegisterProtectedApplicationRawRequest(
                                                "universidad-uco", "gestion-academica", "estudiantes", "consultar"))))
                .flatMap(interactor::execute)
                .map(ProtectedApplicationResponseMapper::toResponse);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.applicationId()).isEqualTo(APPLICATION_ID.value().toString());
                    assertThat(response.resourceId()).isEqualTo(RESOURCE_ID.value().toString());
                    assertThat(response.tenantId()).isEqualTo("universidad-uco");
                    assertThat(response.resourceCode()).isEqualTo("estudiantes");
                    assertThat(response.registeredAt()).isEqualTo(AT);
                })
                .verifyComplete();

        assertThat(received.get().tenantId()).isEqualTo(new TenantId("universidad-uco"));
        assertThat(received.get().resourceCode()).isEqualTo(new ResourceCode("estudiantes"));
    }

    @Test
    void search_maps_raw_request_and_carries_window_metadata_into_the_response() {
        SearchProtectedApplicationsInteractor interactor = dto ->
                Mono.just(ResultPage.of(List.of(entry()), 12, dto.window()));

        StepVerifier.create(Mono.fromSupplier(() ->
                                SearchProtectedApplicationsRequestMapper.toRequest(
                                        SearchProtectedApplicationsRequestMapper.toValidatedRequest(
                                                new SearchProtectedApplicationsRawRequest(
                                                        null, null, null, "1", "5", null, null))))
                        .flatMap(interactor::execute)
                        .map(ProtectedApplicationResponseMapper::toPageResponse))
                .assertNext(page -> {
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.total()).isEqualTo(12);
                    assertThat(page.page()).isEqualTo(1);
                    assertThat(page.size()).isEqualTo(5);
                    assertThat(page.offset()).isEqualTo(5);
                    assertThat(page.limit()).isEqualTo(5);
                })
                .verifyComplete();
    }

    @Test
    void search_passes_criteria_to_interactor_untouched() {
        AtomicReference<SearchProtectedApplicationsRequest> received = new AtomicReference<>();
        SearchProtectedApplicationsInteractor interactor = dto -> {
            received.set(dto);
            return Mono.just(ResultPage.of(List.of(), 0, dto.window()));
        };

        Mono.fromSupplier(() ->
                        SearchProtectedApplicationsRequestMapper.toRequest(
                                SearchProtectedApplicationsRequestMapper.toValidatedRequest(
                                        new SearchProtectedApplicationsRawRequest(
                                                "universidad-uco", "academica", "estud", null, null, null, null))))
                .flatMap(interactor::execute)
                .block();

        assertThat(received.get().criteria().tenantId()).contains(new TenantId("universidad-uco"));
        assertThat(received.get().criteria().nameContains()).contains("academica");
        assertThat(received.get().criteria().resourceContains()).contains("estud");
        assertThat(received.get().window()).isEqualTo(PageWindow.defaultWindow());
    }

    private static co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.response.ProtectedApplicationResponse entry() {
        return new co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.response.ProtectedApplicationResponse(
                APPLICATION_ID,
                RESOURCE_ID,
                new TenantId("universidad-uco"),
                new ApplicationName("gestion-academica"),
                new ResourceCode("estudiantes"),
                new ActionCode("consultar"),
                AT);
    }
}
