package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Sin implementación explícita, quitar un rol de un perfil o revocar un recurso falla con claridad. */
class UnconfiguredRevocationDefaultsTests {

    @Test
    void removing_a_role_from_a_profile_is_not_configured_by_default() {
        AdministerProfileRoleAdditionInteractor interactor = input -> Mono.empty();

        StepVerifier.create(interactor.remove(null))
                .expectErrorSatisfies(error -> org.assertj.core.api.Assertions.assertThat(error)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("no está configurada"))
                .verify();
    }

    @Test
    void revoking_a_resource_grant_is_not_configured_by_default() {
        AdministerResourceGrantInteractor interactor = input -> Mono.empty();

        StepVerifier.create(interactor.revoke(null))
                .expectErrorSatisfies(error -> org.assertj.core.api.Assertions.assertThat(error)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("no está configurada"))
                .verify();
    }
}
