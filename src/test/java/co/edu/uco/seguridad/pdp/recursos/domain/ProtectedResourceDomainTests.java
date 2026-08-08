package co.edu.uco.seguridad.pdp.recursos.domain;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.domain.exception.InvalidActionCodeException;
import co.edu.uco.seguridad.pdp.recursos.domain.exception.InvalidResourceCodeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Dominio del catálogo: formatos de objetos de valor, invariantes de entidades y especificación de consultas.
 * Nada aquí toca Spring, Reactor o HTTP.
 */
class ProtectedResourceDomainTests {

    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final Instant AT = Instant.parse("2026-08-07T12:00:00Z");

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Estudiantes", "1estudiantes", "estudiantes_activos", "e", "  "})
    void resource_code_rejects_anything_that_is_not_lowercase_kebab_case(String candidate) {
        assertThatThrownBy(() -> new ResourceCode(candidate))
                .isInstanceOf(InvalidResourceCodeException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Consultar", "consultar!", "c"})
    void action_code_rejects_anything_that_is_not_lowercase_kebab_case(String candidate) {
        assertThatThrownBy(() -> new ActionCode(candidate))
                .isInstanceOf(InvalidActionCodeException.class);
    }

    @Test
    void resource_code_accepts_hyphenated_lowercase_values() {
        assertThat(new ResourceCode("estudiantes-activos").value()).isEqualTo("estudiantes-activos");
    }

    @Test
    void protected_resource_refuses_to_exist_without_every_part() {
        assertThatThrownBy(() -> ProtectedResource.register(
                new ResourceId(UUID.randomUUID()), APPLICATION, new TenantId("uco"),
                new ApplicationName("gestion-academica"), new ResourceCode("estudiantes"), null, AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void protected_resource_recognises_the_same_grant() {
        ProtectedResource resource = resource("uco", "gestion-academica", "estudiantes", "consultar");

        assertThat(resource.isSameGrantAs(APPLICATION, new ResourceCode("estudiantes"), new ActionCode("consultar")))
                .isTrue();
        assertThat(resource.isSameGrantAs(APPLICATION, new ResourceCode("estudiantes"), new ActionCode("editar")))
                .isFalse();
    }

    @Test
    void unfiltered_criteria_match_everything() {
        assertThat(ProtectedApplicationCriteria.unfiltered()
                .matches(resource("uco", "gestion-academica", "estudiantes", "consultar")))
                .isTrue();
    }

    @Test
    void each_present_filter_restricts_and_each_absent_one_does_not() {
        ProtectedResource resource = resource("uco", "gestion-academica", "estudiantes", "consultar");

        assertThat(criteria("uco", null, null).matches(resource)).isTrue();
        assertThat(criteria("otro", null, null).matches(resource)).isFalse();
        assertThat(criteria(null, "academica", null).matches(resource)).isTrue();
        assertThat(criteria(null, "nomina", null).matches(resource)).isFalse();
        assertThat(criteria(null, null, "estudi").matches(resource)).isTrue();
        assertThat(criteria(null, null, "docentes").matches(resource)).isFalse();
    }

    @Test
    void combined_filters_are_conjunctive() {
        ProtectedResource resource = resource("uco", "gestion-academica", "estudiantes", "consultar");

        assertThat(criteria("uco", "academica", "estudi").matches(resource)).isTrue();
        assertThat(criteria("uco", "academica", "docentes").matches(resource)).isFalse();
    }

    @Test
    void blank_fragments_are_treated_as_absent_rather_than_as_a_match_on_empty_text() {
        ProtectedApplicationCriteria criteria =
                new ProtectedApplicationCriteria(Optional.empty(), Optional.of("   "), Optional.empty());

        assertThat(criteria.nameContains()).isEmpty();
    }

    private static ProtectedApplicationCriteria criteria(String tenant, String name, String resource) {
        return new ProtectedApplicationCriteria(
                Optional.ofNullable(tenant).map(TenantId::new),
                Optional.ofNullable(name),
                Optional.ofNullable(resource));
    }

    private static ProtectedResource resource(String tenant, String name, String code, String action) {
        return ProtectedResource.register(
                new ResourceId(UUID.randomUUID()),
                APPLICATION,
                new TenantId(tenant),
                new ApplicationName(name),
                new ResourceCode(code),
                new ActionCode(action),
                AT);
    }
}
