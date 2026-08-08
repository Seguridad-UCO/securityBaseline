package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.audit;

import co.edu.uco.seguridad.pdp.PdpApplication;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;
import co.edu.uco.seguridad.pdp.recursos.domain.event.ProtectedResourceRegistered;
import co.edu.uco.seguridad.shared.event.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de publicación/consumo entre el caso de uso de registro y la auditoría: publica el evento
 * por el mismo puerto ({@link DomainEventPublisher}) que usa
 * {@code RegisterProtectedApplicationUseCaseImpl} y verifica que el listener de auditoría
 * ({@link InMemoryAuditAdapter}) lo recibe — a través del {@code ApplicationEventPublisher} real de
 * Spring, no de una llamada directa (ver ADR-0002). La entrega es síncrona: no hace falta esperar.
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProtectedResourceAuditListenerTests {

    @Autowired
    private DomainEventPublisher events;

    @Autowired
    private InMemoryAuditAdapter audit;

    @Test
    void the_audit_listener_records_a_published_registration_event() {
        ProtectedResourceRegistered event = new ProtectedResourceRegistered(
                new ResourceId(UUID.randomUUID()),
                new ApplicationId(UUID.randomUUID()),
                new TenantId("universidad-uco"),
                new ResourceCode("estudiantes"),
                new ActionCode("consultar"),
                Instant.now());

        events.publish(event).block();

        assertThat(audit.recorded()).anySatisfy(entry -> {
            assertThat(entry.event()).isEqualTo("protected_application.registered");
            assertThat(entry.tenantId()).isEqualTo("universidad-uco");
            assertThat(entry.resourceId()).isEqualTo(event.resourceId().value().toString());
        });
    }
}
