package co.edu.uco.seguridad.shared.persistence.surrealdb;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code idPart} es lo único entre cada mapper y el formato real de SurrealDB
 * ({@code tabla:valor}, con comillas invertidas cuando el valor no es un identificador simple) —
 * un error acá corrompe silenciosamente to-do id que se lea de vuelta.
 */
class SurrealRecordIdTests {

    @Test
    void extracts_a_simple_identifier_after_the_table_name() {
        assertThat(SurrealRecordId.idPart("tenant:universidad-uco")).isEqualTo("universidad-uco");
    }

    @Test
    void strips_backticks_from_a_quoted_identifier() {
        assertThat(SurrealRecordId.idPart("tenant:`universidad-uco`")).isEqualTo("universidad-uco");
    }

    @Test
    void leaves_a_single_backtick_untouched_because_it_cannot_be_a_quoted_pair() {
        assertThat(SurrealRecordId.idPart("tenant:`")).isEqualTo("`");
    }

    @Test
    void extracts_a_uuid_style_value_unquoted() {
        assertThat(SurrealRecordId.idPart("protected_resource:018f3b1a-1c1a-7c3e-9b1a-000000000000"))
                .isEqualTo("018f3b1a-1c1a-7c3e-9b1a-000000000000");
    }
}
