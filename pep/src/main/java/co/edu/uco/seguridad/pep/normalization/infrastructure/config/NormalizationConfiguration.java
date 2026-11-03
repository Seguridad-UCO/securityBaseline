package co.edu.uco.seguridad.pep.normalization.infrastructure.config;

import co.edu.uco.seguridad.pep.normalization.application.rule.NormalizeAccessRequestMustBeCompleteRule;
import co.edu.uco.seguridad.pep.normalization.application.rule.impl.NormalizeAccessRequestMustBeCompleteRuleImpl;
import co.edu.uco.seguridad.pep.normalization.application.rulesvalidator.NormalizeAccessRulesValidator;
import co.edu.uco.seguridad.pep.normalization.application.rulesvalidator.impl.NormalizeAccessRulesValidatorImpl;
import co.edu.uco.seguridad.pep.normalization.application.usecase.NormalizeAccessUseCase;
import co.edu.uco.seguridad.pep.normalization.application.usecase.impl.NormalizeAccessUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class NormalizationConfiguration {

    @Bean
    NormalizeAccessRequestMustBeCompleteRule normalizeAccessRequestMustBeCompleteRule() {
        return new NormalizeAccessRequestMustBeCompleteRuleImpl();
    }

    @Bean
    NormalizeAccessRulesValidator normalizeAccessRulesValidator(NormalizeAccessRequestMustBeCompleteRule rule) {
        return new NormalizeAccessRulesValidatorImpl(rule);
    }

    @Bean
    NormalizeAccessUseCase normalizeAccessUseCase(NormalizeAccessRulesValidator rules) {
        return new NormalizeAccessUseCaseImpl(rules);
    }

}
