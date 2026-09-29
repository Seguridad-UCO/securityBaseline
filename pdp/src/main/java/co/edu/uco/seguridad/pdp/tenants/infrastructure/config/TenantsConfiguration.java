package co.edu.uco.seguridad.pdp.tenants.infrastructure.config;

import co.edu.uco.seguridad.pdp.tenants.application.rule.validator.TenantMustBeActiveValidator;
import co.edu.uco.seguridad.pdp.tenants.application.rule.validator.impl.TenantMustBeActiveValidatorImpl;
import co.edu.uco.seguridad.pdp.tenants.application.secondaryport.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.CreateTenantUseCase;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.ListTenantsUseCase;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.impl.CreateTenantUseCaseImpl;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.impl.ListTenantsUseCaseImpl;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.TenantCodeMustBeUniqueRule;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.TenantMustExistRule;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.TenantStatusMustBeActiveRule;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.impl.TenantCodeMustBeUniqueRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.impl.TenantMustExistRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.impl.TenantStatusMustBeActiveRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.interactor.CreateTenantInteractor;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.interactor.ListTenantsInteractor;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.interactor.impl.CreateTenantInteractorImpl;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.interactor.impl.ListTenantsInteractorImpl;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.repository.SurrealTenantRepository;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.schema.SurrealTenantSchemaInitializer;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.properties.TenantCatalogProperties;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El único lugar en el módulo que conoce Spring. El cableado vive aquí para que las clases de aplicación y dominio
 * permanezcan como Java simple y puedan ser instanciadas directamente en pruebas.
 */
@Configuration
@EnableConfigurationProperties(TenantCatalogProperties.class)
public class TenantsConfiguration {

    @Bean
    TenantRepository tenantRepository(SurrealDbClient client) {
        return new SurrealTenantRepository(client);
    }

    @Bean
    ApplicationRunner tenantSchemaInitializer(SurrealDbClient client, TenantCatalogProperties properties) {
        return new SurrealTenantSchemaInitializer(client, properties);
    }

    @Bean
    TenantMustExistRule tenantMustExistRule() {
        return new TenantMustExistRuleImpl();
    }

    @Bean
    TenantStatusMustBeActiveRule tenantStatusMustBeActiveRule() {
        return new TenantStatusMustBeActiveRuleImpl();
    }

    @Bean
    TenantCodeMustBeUniqueRule tenantCodeMustBeUniqueRule() {
        return new TenantCodeMustBeUniqueRuleImpl();
    }

    @Bean
    TenantMustBeActiveValidator tenantMustBeActiveValidator(TenantRepository repository,
                                                            TenantMustExistRule mustExist,
                                                            TenantStatusMustBeActiveRule statusMustBeActive) {
        return new TenantMustBeActiveValidatorImpl(repository, mustExist, statusMustBeActive);
    }

    @Bean
    CreateTenantUseCase createTenantUseCase(TenantCodeMustBeUniqueRule mustBeUnique, TenantRepository repository) {
        return new CreateTenantUseCaseImpl(mustBeUnique, repository);
    }

    @Bean
    ListTenantsUseCase listTenantsUseCase(TenantRepository repository) {
        return new ListTenantsUseCaseImpl(repository);
    }

    @Bean
    CreateTenantInteractor createTenantInteractor(CreateTenantUseCase useCase) {
        return new CreateTenantInteractorImpl(useCase);
    }

    @Bean
    ListTenantsInteractor listTenantsInteractor(ListTenantsUseCase useCase) {
        return new ListTenantsInteractorImpl(useCase);
    }
}
