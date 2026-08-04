package co.edu.uco.seguridad.applications.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProtectedApplicationTests {
    @Test void aggregate_requires_exactly_one_valid_initial_resource() {
        var app = ProtectedApplication.register(new ProtectedApplicationId(UUID.randomUUID()), new ApplicationName("Orders API"), new TenantId("tenant-a"), new ResourceIdentifier("/orders"), Instant.now());
        assertEquals(1, app.resources().size());
        assertThrows(DomainException.class, () -> new ResourceIdentifier("orders"));
    }
    @Test void tenant_id_is_a_validated_value_object() {
        assertThrows(DomainException.class, () -> new TenantId("bad tenant"));
    }
}
