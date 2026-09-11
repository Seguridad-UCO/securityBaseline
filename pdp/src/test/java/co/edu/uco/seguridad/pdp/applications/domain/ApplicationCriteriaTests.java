package co.edu.uco.seguridad.pdp.applications.domain;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationCriteriaTests {

    private static final TenantId UCO = new TenantId("universidad-uco");
    private static final TenantId OTRO = new TenantId("otra-universidad");

    @Test
    void without_a_name_filter_accepts_any_application_of_the_tenant() {
        ApplicationCriteria criteria = ApplicationCriteria.ofTenant(UCO);

        assertThat(criteria.matches(application(UCO, "gestion-academica"))).isTrue();
        assertThat(criteria.matches(application(UCO, "portal-estudiante"))).isTrue();
    }

    @Test
    void accepts_an_application_whose_name_contains_the_fragment() {
        ApplicationCriteria criteria = ApplicationCriteria.of(UCO, Optional.of("portal"));

        assertThat(criteria.matches(application(UCO, "portal-estudiante"))).isTrue();
    }

    @Test
    void matches_the_fragment_ignoring_case() {
        ApplicationCriteria criteria = ApplicationCriteria.of(UCO, Optional.of("PORTAL"));

        assertThat(criteria.matches(application(UCO, "portal-estudiante"))).isTrue();
    }

    @Test
    void rejects_an_application_whose_name_does_not_contain_the_fragment() {
        ApplicationCriteria criteria = ApplicationCriteria.of(UCO, Optional.of("portal"));

        assertThat(criteria.matches(application(UCO, "gestion-academica"))).isFalse();
    }

    @Test
    void rejects_an_application_of_another_tenant_even_without_a_filter() {
        ApplicationCriteria criteria = ApplicationCriteria.ofTenant(UCO);

        assertThat(criteria.matches(application(OTRO, "gestion-academica"))).isFalse();
    }

    @Test
    void rejects_an_application_of_another_tenant_that_matches_the_fragment() {
        ApplicationCriteria criteria = ApplicationCriteria.of(UCO, Optional.of("portal"));

        assertThat(criteria.matches(application(OTRO, "portal-estudiante"))).isFalse();
    }

    @Test
    void requires_a_tenant() {
        assertThatThrownBy(() -> ApplicationCriteria.ofTenant(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void requires_the_optional_filter_not_to_be_null() {
        assertThatThrownBy(() -> ApplicationCriteria.of(UCO, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static Application application(TenantId tenantId, String name) {
        return new Application(new ApplicationId(UUID.randomUUID()), tenantId, new ApplicationName(name), "",
                new ApplicationBaseUrl("https://example.com"), Instant.parse("2026-01-01T00:00:00Z"));
    }
}
