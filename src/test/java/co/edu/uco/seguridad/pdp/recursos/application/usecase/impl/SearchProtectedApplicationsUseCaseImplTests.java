package co.edu.uco.seguridad.pdp.recursos.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.SearchProtectedApplicationsRequest;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.impl.SearchProtectedApplicationsRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedApplicationCriteria;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import co.edu.uco.seguridad.pdp.tenants.TenantStatus;
import co.edu.uco.seguridad.pdp.tenants.application.exception.TenantNotFoundException;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Criterios 16 a 19 de extremo a extremo a través del caso de uso: una consulta dinámica, ventanas acotadas, y un
 * total que es independiente de la porción devuelta.
 */
class SearchProtectedApplicationsUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");

    private FakeProtectedResourceRepository resources;
    private SearchProtectedApplicationsUseCaseImpl service;

    @BeforeEach
    void setUp() {
        resources = new FakeProtectedResourceRepository();
        TenantMustBeActiveRule activeTenant = tenantId -> TENANT.equals(tenantId)
                ? Mono.just(new TenantResponse(tenantId, TenantStatus.ACTIVE))
                : Mono.error(new TenantNotFoundException(tenantId));
        service = new SearchProtectedApplicationsUseCaseImpl(
                new SearchProtectedApplicationsRulesValidatorImpl(activeTenant), resources);

        store("gestion-academica", "estudiantes", "consultar", 0);
        store("gestion-academica", "docentes", "consultar", 1);
        store("nomina", "empleados", "editar", 2);
    }

    @Test
    void returns_every_row_when_no_filter_is_supplied() {
        StepVerifier.create(service.execute(dto(ProtectedApplicationCriteria.scopedTo(TENANT), PageWindow.defaultWindow())))
                .assertNext(page -> {
                    assertThat(page.content()).hasSize(3);
                    assertThat(page.total()).isEqualTo(3);
                })
                .verifyComplete();
    }

    @Test
    void applies_only_the_filters_that_are_present() {
        ProtectedApplicationCriteria byName = new ProtectedApplicationCriteria(
                TENANT, Optional.of("academica"), Optional.empty());

        StepVerifier.create(service.execute(dto(byName, PageWindow.defaultWindow())))
                .assertNext(page -> assertThat(page.content())
                        .extracting(ProtectedResource::code)
                        .containsExactly(new ResourceCode("estudiantes"), new ResourceCode("docentes")))
                .verifyComplete();
    }

    @Test
    void reports_the_full_total_even_when_the_window_returns_one_row() {
        StepVerifier.create(service.execute(
                        dto(ProtectedApplicationCriteria.scopedTo(TENANT), PageWindow.ofRange(0, 1))))
                .assertNext(page -> {
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.total()).isEqualTo(3);
                })
                .verifyComplete();
    }

    @Test
    void pages_are_stable_and_do_not_overlap() {
        var first = service.execute(dto(ProtectedApplicationCriteria.scopedTo(TENANT), PageWindow.ofPage(0, 2))).block();
        var second = service.execute(dto(ProtectedApplicationCriteria.scopedTo(TENANT), PageWindow.ofPage(1, 2))).block();

        assertThat(first.content()).hasSize(2);
        assertThat(second.content()).hasSize(1);
        assertThat(second.content()).doesNotContainAnyElementsOf(first.content());
    }

    @Test
    void rejects_a_search_scoped_to_an_unknown_tenant_instead_of_returning_an_empty_page() {
        ProtectedApplicationCriteria unknownTenant =
                ProtectedApplicationCriteria.scopedTo(new TenantId("inexistente"));

        StepVerifier.create(service.execute(dto(unknownTenant, PageWindow.defaultWindow())))
                .expectError(TenantNotFoundException.class)
                .verify();
    }

    private static SearchProtectedApplicationsRequest dto(ProtectedApplicationCriteria criteria, PageWindow window) {
        return new SearchProtectedApplicationsRequest(criteria, window);
    }

    private void store(String application, String code, String action, int secondsOffset) {
        resources.save(ProtectedResource.register(
                new ResourceId(UUID.randomUUID()),
                new ApplicationId(UUID.randomUUID()),
                TENANT,
                new ApplicationName(application),
                new ResourceCode(code),
                new ActionCode(action),
                Instant.parse("2026-08-07T12:00:00Z").plusSeconds(secondsOffset))).block();
    }
}
