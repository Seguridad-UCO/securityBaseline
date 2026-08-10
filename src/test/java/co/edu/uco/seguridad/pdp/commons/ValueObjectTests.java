package co.edu.uco.seguridad.pdp.commons;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidApplicationNameException;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidIdentifierException;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidTenantIdException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariantes de los objetos de valor del núcleo compartido. Se ejecutan sin Spring y sin Reactor,
 * que es el punto: estas reglas se cumplen en cualquier lugar donde se use el tipo.
 */
class ValueObjectTests {

    @Test
    void applicationName_is_the_canonical_example_of_a_trimmed_value_object() {
        assertThat(new ApplicationName("  gestion-academica  ").value()).isEqualTo("gestion-academica");
    }

    @Nested
    @DisplayName("TenantId")
    class TenantIdTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "a", "with space", "sí-acentos", "tenant!"})
        void rejects_values_that_are_absent_or_malformed(String candidate) {
            assertThatThrownBy(() -> new TenantId(candidate))
                    .isInstanceOf(InvalidTenantIdException.class)
                    .extracting(error -> ((InvalidTenantIdException) error).code())
                    .isEqualTo("INVALID_TENANT_ID");
        }

        @Test
        void trims_surrounding_whitespace_so_equality_is_not_defeated_by_formatting() {
            assertThat(new TenantId("  universidad-uco  ")).isEqualTo(new TenantId("universidad-uco"));
        }
    }

    @Nested
    @DisplayName("ApplicationName")
    class ApplicationNameTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "ab"})
        void rejects_absent_or_too_short_names(String candidate) {
            assertThatThrownBy(() -> new ApplicationName(candidate))
                    .isInstanceOf(InvalidApplicationNameException.class);
        }

        @Test
        void rejects_names_longer_than_one_hundred_characters() {
            assertThatThrownBy(() -> new ApplicationName("x".repeat(101)))
                    .isInstanceOf(InvalidApplicationNameException.class);
        }

        @Test
        void accepts_the_boundary_lengths() {
            assertThat(new ApplicationName("abc").value()).isEqualTo("abc");
            assertThat(new ApplicationName("x".repeat(100)).value()).hasSize(100);
        }

        @Test
        void compares_case_insensitively_because_uniqueness_is_functional() {
            assertThat(new ApplicationName("Gestion-Academica").sameAs(new ApplicationName("gestion-academica")))
                    .isTrue();
        }

        @Test
        void matches_fragments_case_insensitively() {
            assertThat(new ApplicationName("gestion-academica").contains("ACADEMICA")).isTrue();
            assertThat(new ApplicationName("gestion-academica").contains("nomina")).isFalse();
        }
    }

    @Nested
    @DisplayName("Identifiers")
    class IdentifierTests {

        @Test
        void reject_null_values() {
            assertThatThrownBy(() -> new ApplicationId(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ResourceId(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void parse_well_formed_text_and_reject_the_rest() {
            UUID value = UUID.randomUUID();
            assertThat(ApplicationId.of(value.toString()).value()).isEqualTo(value);
            assertThatThrownBy(() -> ResourceId.of("not-a-uuid"))
                    .isInstanceOf(InvalidIdentifierException.class);
        }
    }
}
