package co.edu.uco.seguridad.pdp.tenants.infrastructure.config;

import co.edu.uco.seguridad.pdp.tenants.TenantModuleApi;
import co.edu.uco.seguridad.pdp.tenants.application.TenantLookupService;
import co.edu.uco.seguridad.pdp.tenants.application.port.secondary.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import co.edu.uco.seguridad.pdp.tenants.application.rule.impl.TenantMustBeActiveRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.FindTenantUseCase;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.impl.FindTenantUseCaseImpl;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.entity.TenantEntity;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.repository.InMemoryTenantRepository;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.properties.TenantCatalogProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El único lugar en el módulo que conoce Spring. El cableado vive aquí para que las clases de aplicación y dominio
 * permanezcan como Java simple y puedan ser instanciadas directamente en pruebas.
 */
@Configuration
@EnableConfigurationProperties(TenantCatalogProperties.class)
public class TenantConfiguration {

    @Bean
    TenantRepository tenantRepository(TenantCatalogProperties properties) {
        return new InMemoryTenantRepository(properties.seed().entrySet().stream()
                .map(entry -> new TenantEntity(entry.getKey(), entry.getValue()))
                .toList());
    }

    @Bean
    FindTenantUseCase findTenantUseCase(TenantRepository repository) {
        return new FindTenantUseCaseImpl(repository);
    }

    @Bean
    TenantModuleApi tenantModuleApi(FindTenantUseCase findTenantUseCase) {
        return new TenantLookupService(findTenantUseCase);
    }

    @Bean
    TenantMustBeActiveRule tenantMustBeActiveRule(TenantRepository repository) {
        return new TenantMustBeActiveRuleImpl(repository);
    }
}
