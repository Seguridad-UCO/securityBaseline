package co.edu.uco.seguridad.pdp.applications.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.port.secondary.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.application.rule.ApplicationNameMustBeUniqueForTenantRule;
import co.edu.uco.seguridad.pdp.applications.application.rule.ApplicationNameMustNotBeReservedRule;
import co.edu.uco.seguridad.pdp.applications.application.rule.impl.ApplicationNameMustBeUniqueForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.applications.application.rule.impl.ApplicationNameMustNotBeReservedRuleImpl;
import co.edu.uco.seguridad.pdp.applications.application.rulesvalidator.RegisterApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.applications.application.rulesvalidator.impl.RegisterApplicationRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.applications.application.usecase.ListApplicationsUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.impl.ListApplicationsUseCaseImpl;
import co.edu.uco.seguridad.pdp.applications.application.usecase.impl.RegisterApplicationUseCaseImpl;
import co.edu.uco.seguridad.pdp.applications.application.usecase.impl.RemoveApplicationUseCaseImpl;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.repository.SurrealApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.schema.SurrealApplicationSchemaInitializer;
import co.edu.uco.seguridad.pdp.applications.infrastructure.properties.ApplicationCatalogProperties;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La única clase consciente de Spring en el módulo. Cada regla es un bean para que pueda ser reemplazada o
 * decorada sin editar el validador que la compone.
 */
@Configuration
@EnableConfigurationProperties(ApplicationCatalogProperties.class)
public class ApplicationsConfiguration {

    @Bean
    ApplicationRepository applicationRepository(SurrealDbClient client) {
        return new SurrealApplicationRepository(client);
    }

    @Bean
    ApplicationRunner applicationSchemaInitializer(SurrealDbClient client) {
        return new SurrealApplicationSchemaInitializer(client);
    }

    @Bean
    ApplicationNameMustNotBeReservedRule applicationNameMustNotBeReservedRule(ApplicationCatalogProperties properties) {
        return new ApplicationNameMustNotBeReservedRuleImpl(properties.reservedNames());
    }

    @Bean
    ApplicationNameMustBeUniqueForTenantRule applicationNameMustBeUniqueForTenantRule(ApplicationRepository repository) {
        return new ApplicationNameMustBeUniqueForTenantRuleImpl(repository);
    }

    @Bean
    RegisterApplicationRulesValidator registerApplicationRulesValidator(
            ApplicationNameMustNotBeReservedRule nameMustNotBeReserved,
            TenantMustBeActiveRule tenantMustBeActive,
            ApplicationNameMustBeUniqueForTenantRule nameMustBeUnique) {
        return new RegisterApplicationRulesValidatorImpl(nameMustNotBeReserved, tenantMustBeActive, nameMustBeUnique);
    }

    @Bean
    RegisterApplicationUseCase registerApplicationUseCase(RegisterApplicationRulesValidator rules,
                                                          ApplicationRepository repository,
                                                          IdentifierGenerator identifiers,
                                                          TimeProvider time) {
        return new RegisterApplicationUseCaseImpl(rules, repository, identifiers, time);
    }

    @Bean
    ListApplicationsUseCase listApplicationsUseCase(ApplicationRepository repository) {
        return new ListApplicationsUseCaseImpl(repository);
    }

    @Bean
    RemoveApplicationUseCase removeApplicationUseCase(ApplicationRepository repository) {
        return new RemoveApplicationUseCaseImpl(repository);
    }
}
