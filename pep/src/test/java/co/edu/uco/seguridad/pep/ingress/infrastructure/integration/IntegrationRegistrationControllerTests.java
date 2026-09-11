package co.edu.uco.seguridad.pep.ingress.infrastructure.integration;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.application.rule.impl.IntegrationCredentialMustMatchRuleImpl;
import co.edu.uco.seguridad.pep.ingress.application.rule.impl.IntegrationRegistrationMustBeEnabledRuleImpl;
import co.edu.uco.seguridad.pep.ingress.application.rulesvalidator.impl.RegisterIntegrationRulesValidatorImpl;
import co.edu.uco.seguridad.pep.ingress.application.usecase.impl.RegisterIntegrationUseCaseImpl;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.controller.IntegrationRegistrationController;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.interactor.impl.RegisterIntegrationInteractorImpl;
import co.edu.uco.seguridad.pep.ingress.infrastructure.properties.IngressProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationRegistrationControllerTests {
    @TempDir Path temporaryDirectory;

    @Test void accepts_only_the_token_assigned_to_the_application_environment() {
        var controller = controller("registration-secret");

        var response = controller.register("academic", "dev", "Bearer registration-secret",
                new IntegrationRegistrationController.RegistrationBody(URI.create("http://academic.internal"), "academic-api")).block();

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.publicBaseUrl()).isEqualTo(URI.create("https://security.example.edu/apps/academic"));
        assertThatThrownBy(() -> controller.register("academic", "dev", "Bearer wrong",
                new IntegrationRegistrationController.RegistrationBody(URI.create("http://academic.internal"), "academic-api")).block())
                .isInstanceOf(EnforcementFailure.class);
    }

    private IntegrationRegistrationController controller(String token) {
        var properties = new IntegrationProperties(true, temporaryDirectory.resolve("routes.json"), URI.create("https://security.example.edu"),
                List.of(new IntegrationProperties.Credential("academic", "dev", new BCryptPasswordEncoder().encode(token))));
        var ingress = new IngressProperties(List.of(), "http://issuer.example.edu", URI.create("http://issuer.example.edu/jwks"),
                true, List.of(), 1, 1, 1, 1, 1, Duration.ofSeconds(1));
        var registry = new RouteRegistry(properties, ingress, JsonMapper.builder().build());
        var rules = new RegisterIntegrationRulesValidatorImpl(
                new IntegrationRegistrationMustBeEnabledRuleImpl(registry),
                new IntegrationCredentialMustMatchRuleImpl(registry));
        var useCase = new RegisterIntegrationUseCaseImpl(rules, registry);
        return new IntegrationRegistrationController(new RegisterIntegrationInteractorImpl(useCase));
    }
}
