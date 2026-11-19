package co.edu.uco.seguridad.shared.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OidcReturnTargetPolicyTests {
    private final OidcReturnTargetPolicy policy = new OidcReturnTargetPolicy(List.of("http://localhost:5174"));

    @Test
    void accepts_a_path_and_query_on_an_registered_origin() {
        assertThat(policy.validate("http://localhost:5174/grades?tab=current"))
                .isEqualTo("http://localhost:5174/grades?tab=current");
    }

    @Test
    void rejects_an_unregistered_origin() {
        assertThatThrownBy(() -> policy.validate("https://attacker.example/steal"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rejects_a_fragment_that_could_smuggle_a_second_destination() {
        assertThatThrownBy(() -> policy.validate("http://localhost:5174/#https://attacker.example"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
