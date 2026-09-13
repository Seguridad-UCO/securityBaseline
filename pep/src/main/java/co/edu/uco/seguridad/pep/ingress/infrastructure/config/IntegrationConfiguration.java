package co.edu.uco.seguridad.pep.ingress.infrastructure.config;

import co.edu.uco.seguridad.pep.ingress.application.port.secondary.IntegrationRegistrationPort;
import co.edu.uco.seguridad.pep.ingress.application.port.secondary.ApplicationCredentialValidationPort;
import co.edu.uco.seguridad.pep.ingress.application.rule.IntegrationCredentialMustMatchRule;
import co.edu.uco.seguridad.pep.ingress.application.rule.IntegrationRegistrationMustBeEnabledRule;
import co.edu.uco.seguridad.pep.ingress.application.rule.impl.IntegrationCredentialMustMatchRuleImpl;
import co.edu.uco.seguridad.pep.ingress.application.rule.impl.IntegrationRegistrationMustBeEnabledRuleImpl;
import co.edu.uco.seguridad.pep.ingress.application.rulesvalidator.RegisterIntegrationRulesValidator;
import co.edu.uco.seguridad.pep.ingress.application.rulesvalidator.impl.RegisterIntegrationRulesValidatorImpl;
import co.edu.uco.seguridad.pep.ingress.application.usecase.RegisterIntegrationUseCase;
import co.edu.uco.seguridad.pep.ingress.application.usecase.impl.RegisterIntegrationUseCaseImpl;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.secondary.http.PdpApplicationCredentialValidationAdapter;
import co.edu.uco.seguridad.pep.ingress.infrastructure.integration.IntegrationProperties;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.interactor.RegisterIntegrationInteractor;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.interactor.impl.RegisterIntegrationInteractorImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;

/** Ensambla los puertos, reglas y caso de uso del plano de control de integraciones. */
@Configuration(proxyBeanMethods = false)
class IntegrationConfiguration {

    @Bean
    IntegrationRegistrationMustBeEnabledRule integrationRegistrationMustBeEnabledRule(IntegrationRegistrationPort port) {
        return new IntegrationRegistrationMustBeEnabledRuleImpl(port);
    }

    @Bean
    ApplicationCredentialValidationPort applicationCredentialValidationPort(@Qualifier("pdpWebClient") WebClient client,
            IntegrationProperties properties) {
        return new PdpApplicationCredentialValidationAdapter(client, properties);
    }

    @Bean
    IntegrationCredentialMustMatchRule integrationCredentialMustMatchRule(ApplicationCredentialValidationPort credentials) {
        return new IntegrationCredentialMustMatchRuleImpl(credentials);
    }

    @Bean
    RegisterIntegrationRulesValidator registerIntegrationRulesValidator(
            IntegrationRegistrationMustBeEnabledRule enabled, IntegrationCredentialMustMatchRule credential) {
        return new RegisterIntegrationRulesValidatorImpl(enabled, credential);
    }

    @Bean
    RegisterIntegrationUseCase registerIntegrationUseCase(RegisterIntegrationRulesValidator rules,
                                                          IntegrationRegistrationPort port) {
        return new RegisterIntegrationUseCaseImpl(rules, port);
    }

    @Bean
    RegisterIntegrationInteractor registerIntegrationInteractor(RegisterIntegrationUseCase useCase) {
        return new RegisterIntegrationInteractorImpl(useCase);
    }
}
