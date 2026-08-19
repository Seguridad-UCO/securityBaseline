package co.edu.uco.seguridad.pdp.resources.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.resources.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.application.rule.ProtectedResourceMustBeUniqueRule;
import co.edu.uco.seguridad.pdp.resources.application.rule.ProtectedResourceMustBelongToApplicationTenantRule;
import co.edu.uco.seguridad.pdp.resources.application.rule.impl.ProtectedResourceMustBeUniqueRuleImpl;
import co.edu.uco.seguridad.pdp.resources.application.rule.impl.ProtectedResourceMustBelongToApplicationTenantRuleImpl;
import co.edu.uco.seguridad.pdp.resources.application.rulesvalidator.RegisterProtectedApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.resources.application.rulesvalidator.SearchProtectedApplicationsRulesValidator;
import co.edu.uco.seguridad.pdp.resources.application.rulesvalidator.impl.RegisterProtectedApplicationRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.resources.application.rulesvalidator.impl.SearchProtectedApplicationsRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedApplicationUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.SearchProtectedApplicationsUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.impl.RegisterProtectedApplicationUseCaseImpl;
import co.edu.uco.seguridad.pdp.resources.application.usecase.impl.SearchProtectedApplicationsUseCaseImpl;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.RegisterProtectedApplicationInteractor;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.SearchProtectedApplicationsInteractor;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.impl.RegisterProtectedApplicationInteractorImpl;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.impl.SearchProtectedApplicationsInteractorImpl;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.audit.InMemoryAuditAdapter;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.repository.SurrealProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.schema.SurrealProtectedResourceSchemaInitializer;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import co.edu.uco.seguridad.shared.event.DomainEventPublisher;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La única clase consciente de Spring en el módulo. Cablea los puertos secundarios a sus adaptadores
 * y ensambla casos de uso e interactores.
 */
@Configuration
public class ResourcesConfiguration {

    @Bean
    ProtectedResourceRepository protectedResourceRepository(SurrealDbClient client) {
        return new SurrealProtectedResourceRepository(client);
    }

    @Bean
    ApplicationRunner protectedResourceSchemaInitializer(SurrealDbClient client) {
        return new SurrealProtectedResourceSchemaInitializer(client);
    }

    @Bean
    InMemoryAuditAdapter inMemoryAuditAdapter() {
        return new InMemoryAuditAdapter();
    }

    @Bean
    ProtectedResourceMustBelongToApplicationTenantRule protectedResourceMustBelongToApplicationTenantRule() {
        return new ProtectedResourceMustBelongToApplicationTenantRuleImpl();
    }

    @Bean
    ProtectedResourceMustBeUniqueRule protectedResourceMustBeUniqueRule(ProtectedResourceRepository repository) {
        return new ProtectedResourceMustBeUniqueRuleImpl(repository);
    }

    @Bean
    RegisterProtectedApplicationRulesValidator registerProtectedApplicationRulesValidator(
            TenantMustBeActiveRule tenantMustBeActive,
            ProtectedResourceMustBelongToApplicationTenantRule resourceMustBelongToApplicationTenant,
            ProtectedResourceMustBeUniqueRule resourceMustBeUnique) {
        return new RegisterProtectedApplicationRulesValidatorImpl(
                tenantMustBeActive, resourceMustBelongToApplicationTenant, resourceMustBeUnique);
    }

    @Bean
    SearchProtectedApplicationsRulesValidator searchProtectedApplicationsRulesValidator(
            TenantMustBeActiveRule tenantMustBeActive) {
        return new SearchProtectedApplicationsRulesValidatorImpl(tenantMustBeActive);
    }

    @Bean
    RegisterProtectedApplicationUseCase registerProtectedApplicationUseCase(
            RegisterApplicationUseCase registerApplicationUseCase,
            RemoveApplicationUseCase removeApplicationUseCase,
            RegisterProtectedApplicationRulesValidator rules,
            ProtectedResourceRepository resources,
            DomainEventPublisher events,
            IdentifierGenerator identifiers,
            TimeProvider time) {
        return new RegisterProtectedApplicationUseCaseImpl(
                registerApplicationUseCase,
                removeApplicationUseCase,
                rules,
                resources,
                events,
                identifiers,
                time);
    }

    @Bean
    SearchProtectedApplicationsUseCase searchProtectedApplicationsUseCase(
            SearchProtectedApplicationsRulesValidator rules,
            ProtectedResourceRepository resources) {
        return new SearchProtectedApplicationsUseCaseImpl(rules, resources);
    }

    @Bean
    RegisterProtectedApplicationInteractor registerProtectedApplicationInteractor(
            RegisterProtectedApplicationUseCase useCase) {
        return new RegisterProtectedApplicationInteractorImpl(useCase);
    }

    @Bean
    SearchProtectedApplicationsInteractor searchProtectedApplicationsInteractor(
            SearchProtectedApplicationsUseCase useCase) {
        return new SearchProtectedApplicationsInteractorImpl(useCase);
    }
}
