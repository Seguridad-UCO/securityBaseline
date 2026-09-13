package co.edu.uco.seguridad.pdp.applications.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationNameMustBeUniqueForTenantRule;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationNameMustNotBeReservedRule;
import co.edu.uco.seguridad.pdp.applications.domain.rule.impl.ApplicationMustExistForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.applications.domain.rule.impl.ApplicationNameMustBeUniqueForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.applications.domain.rule.impl.ApplicationNameMustNotBeReservedRuleImpl;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.impl.ApplicationMustExistForTenantValidatorImpl;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.impl.ApplicationOwnerLookupValidatorImpl;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.RegisterApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.impl.RegisterApplicationRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.applications.application.usecase.ListApplicationsUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.ValidateApplicationCredentialUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.impl.ListApplicationsUseCaseImpl;
import co.edu.uco.seguridad.pdp.applications.application.usecase.impl.RegisterApplicationUseCaseImpl;
import co.edu.uco.seguridad.pdp.applications.application.usecase.impl.RemoveApplicationUseCaseImpl;
import co.edu.uco.seguridad.pdp.applications.application.usecase.impl.ValidateApplicationCredentialUseCaseImpl;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationCredentialMustBeValidRule;
import co.edu.uco.seguridad.pdp.applications.domain.rule.impl.ApplicationCredentialMustBeValidRuleImpl;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.ListApplicationsInteractor;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.RegisterApplicationInteractor;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.ValidateApplicationCredentialInteractor;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.impl.ListApplicationsInteractorImpl;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.impl.RegisterApplicationInteractorImpl;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.impl.ValidateApplicationCredentialInteractorImpl;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.repository.SurrealApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.schema.SurrealApplicationSchemaInitializer;
import co.edu.uco.seguridad.pdp.applications.infrastructure.properties.ApplicationCatalogProperties;
import co.edu.uco.seguridad.pdp.tenants.application.rule.validator.TenantMustBeActiveValidator;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.port.CredentialHasher;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.SecretGenerator;
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
    ApplicationNameMustBeUniqueForTenantRule applicationNameMustBeUniqueForTenantRule() {
        return new ApplicationNameMustBeUniqueForTenantRuleImpl();
    }

    @Bean
    RegisterApplicationRulesValidator registerApplicationRulesValidator(
            ApplicationNameMustNotBeReservedRule nameMustNotBeReserved,
            TenantMustBeActiveValidator tenantMustBeActive,
            ApplicationNameMustBeUniqueForTenantRule nameMustBeUnique,
            ApplicationRepository repository) {
        return new RegisterApplicationRulesValidatorImpl(nameMustNotBeReserved, tenantMustBeActive, nameMustBeUnique,
                repository);
    }

    @Bean
    RegisterApplicationUseCase registerApplicationUseCase(RegisterApplicationRulesValidator rules,
                                                          ApplicationRepository repository,
                                                          IdentifierGenerator identifiers,
                                                          TimeProvider time,
                                                          SecretGenerator secretGenerator,
                                                          CredentialHasher hasher) {
        return new RegisterApplicationUseCaseImpl(rules, repository, identifiers, time, secretGenerator, hasher);
    }

    @Bean
    ListApplicationsUseCase listApplicationsUseCase(ApplicationRepository repository) {
        return new ListApplicationsUseCaseImpl(repository);
    }

    @Bean
    RemoveApplicationUseCase removeApplicationUseCase(ApplicationRepository repository) {
        return new RemoveApplicationUseCaseImpl(repository);
    }

    @Bean
    RegisterApplicationInteractor registerApplicationInteractor(RegisterApplicationUseCase useCase) {
        return new RegisterApplicationInteractorImpl(useCase);
    }

    @Bean
    ListApplicationsInteractor listApplicationsInteractor(ListApplicationsUseCase useCase) {
        return new ListApplicationsInteractorImpl(useCase);
    }

    @Bean
    ApplicationMustExistForTenantRule applicationMustExistForTenantRule() {
        return new ApplicationMustExistForTenantRuleImpl();
    }

    @Bean
    ApplicationMustExistForTenantValidator applicationMustExistForTenantValidator(ApplicationRepository repository,
                                                                                 ApplicationMustExistForTenantRule mustExist) {
        return new ApplicationMustExistForTenantValidatorImpl(repository, mustExist);
    }

    // HU-003 — canal interno para el PEP: resuelve el tenant dueño a partir solo del applicationId.
    @Bean
    ApplicationOwnerLookupValidator applicationOwnerLookupValidator(ApplicationRepository repository) {
        return new ApplicationOwnerLookupValidatorImpl(repository);
    }

    // HU-013 — canal interno de validación de credenciales.
    @Bean
    ApplicationCredentialMustBeValidRule applicationCredentialMustBeValidRule() {
        return new ApplicationCredentialMustBeValidRuleImpl();
    }

    @Bean
    ValidateApplicationCredentialUseCase validateApplicationCredentialUseCase(
            ApplicationCredentialMustBeValidRule rule, ApplicationRepository repository, CredentialHasher hasher) {
        return new ValidateApplicationCredentialUseCaseImpl(rule, repository, hasher);
    }

    @Bean
    ValidateApplicationCredentialInteractor validateApplicationCredentialInteractor(
            ValidateApplicationCredentialUseCase useCase) {
        return new ValidateApplicationCredentialInteractorImpl(useCase);
    }
}
