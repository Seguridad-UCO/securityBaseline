package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.auth.service.OidcRedirectPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakSecurityConfigurationTests {

    @Test
    void redirects_authentication_failures_back_to_the_frontend() {
        assertThat(new OidcRedirectPolicy("http://localhost:5173").frontendAuthenticationError())
                .hasToString("http://localhost:5173?error=authentication");
    }

    @Test
    void replaces_existing_query_parameters_when_building_the_failure_redirect() {
        assertThat(new OidcRedirectPolicy("http://localhost:5173/login?from=oauth").frontendAuthenticationError())
                .hasToString("http://localhost:5173/login?error=authentication");
    }
}
