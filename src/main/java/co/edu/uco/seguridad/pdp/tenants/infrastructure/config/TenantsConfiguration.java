package co.edu.uco.seguridad.pdp.tenants.infrastructure.config;

import co.edu.uco.seguridad.pdp.tenants.application.port.secondary.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantCodeMustBeUniqueRule;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantStatusMustBeActiveRule;
import co.edu.uco.seguridad.pdp.tenants.application.rule.impl.TenantCodeMustBeUniqueRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.application.rule.impl.TenantMustBeActiveRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.application.rule.impl.TenantStatusMustBeActiveRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.CreateTenantUseCase;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.ListTenantsUseCase;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.impl.CreateTenantUseCaseImpl;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.impl.ListTenantsUseCaseImpl;
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
    TenantStatusMustBeActiveRule tenantStatusMustBeActiveRule() {
        return new TenantStatusMustBeActiveRuleImpl();
    }

    @Bean
    TenantMustBeActiveRule tenantMustBeActiveRule(TenantRepository repository,
                                                  TenantStatusMustBeActiveRule statusMustBeActive) {
        return new TenantMustBeActiveRuleImpl(repository, statusMustBeActive);
    }

    @Bean
    TenantCodeMustBeUniqueRule tenantCodeMustBeUniqueRule(TenantRepository repository) {
        return new TenantCodeMustBeUniqueRuleImpl(repository);
    }

    @Bean
    CreateTenantUseCase createTenantUseCase(TenantCodeMustBeUniqueRule mustBeUnique, TenantRepository repository) {
        return new CreateTenantUseCaseImpl(mustBeUnique, repository);
    }

    @Bean
    ListTenantsUseCase listTenantsUseCase(TenantRepository repository) {
        return new ListTenantsUseCaseImpl(repository);
    }
}
