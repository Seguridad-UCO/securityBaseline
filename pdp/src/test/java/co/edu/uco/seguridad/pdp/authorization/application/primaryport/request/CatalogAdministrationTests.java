package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HU-036: {@link CatalogAdministration} reemplaza a {@code Optional<AdministrationRequest>}: ya no existe el
 * estado «sin gate». O se administra la aplicación del objetivo ({@code OfApplication}) o se administra al
 * menos una aplicación del inquilino ({@code OfTenant}); ninguna de las dos variantes admite {@code null}.
 */
class CatalogAdministrationTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final UserId USER = new UserId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of());
    private static final TenantAdministrationRequest ACTOR =
            new TenantAdministrationRequest(TENANT, USER, "test-subject", AuthenticationContextEvidence.NONE);

    /** Sin {@code default}: si el tipo sellado ganara o perdiera una variante, esto dejaría de compilar. */
    private static String gateOf(CatalogAdministration administration) {
        return switch (administration) {
            case CatalogAdministration.OfApplication application -> "application:" + application.administration().applicationId().value();
            case CatalogAdministration.OfTenant tenant -> "tenant:" + tenant.actor().tenantId().value();
        };
    }

    @Test
    void of_application_carries_the_application_administration() {
        CatalogAdministration administration = new CatalogAdministration.OfApplication(ADMINISTRATION);

        assertThat(administration).isInstanceOfSatisfying(CatalogAdministration.OfApplication.class,
                application -> assertThat(application.administration()).isEqualTo(ADMINISTRATION));
    }

    @Test
    void of_tenant_carries_the_tenant_actor() {
        CatalogAdministration administration = new CatalogAdministration.OfTenant(ACTOR);

        assertThat(administration).isInstanceOfSatisfying(CatalogAdministration.OfTenant.class,
                tenant -> assertThat(tenant.actor()).isEqualTo(ACTOR));
    }

    @Test
    void of_application_rejects_null() {
        assertThatThrownBy(() -> new CatalogAdministration.OfApplication(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void of_tenant_rejects_null() {
        assertThatThrownBy(() -> new CatalogAdministration.OfTenant(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void the_switch_over_the_sealed_type_is_exhaustive_and_tells_the_two_variants_apart() {
        assertThat(gateOf(new CatalogAdministration.OfApplication(ADMINISTRATION)))
                .isEqualTo("application:" + APPLICATION.value());
        assertThat(gateOf(new CatalogAdministration.OfTenant(ACTOR))).isEqualTo("tenant:universidad-uco");
    }
}
