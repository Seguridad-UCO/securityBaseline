package co.edu.uco.seguridad.pdp.recursos.infrastructure.config;

import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.interactor.RegisterApplicationInteractor;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.interactor.RemoveApplicationInteractor;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.RegisterProtectedApplicationInteractor;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.SearchProtectedApplicationsInteractor;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.impl.RegisterProtectedApplicationInteractorImpl;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.impl.SearchProtectedApplicationsInteractorImpl;
import co.edu.uco.seguridad.pdp.recursos.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.recursos.application.rule.ProtectedResourceMustBeUniqueRule;
import co.edu.uco.seguridad.pdp.recursos.application.rule.ProtectedResourceMustBelongToApplicationTenantRule;
import co.edu.uco.seguridad.pdp.recursos.application.rule.impl.ProtectedResourceMustBeUniqueRuleImpl;
import co.edu.uco.seguridad.pdp.recursos.application.rule.impl.ProtectedResourceMustBelongToApplicationTenantRuleImpl;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.RegisterProtectedApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.SearchProtectedApplicationsRulesValidator;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.impl.RegisterProtectedApplicationRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.impl.SearchProtectedApplicationsRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.RegisterProtectedApplicationUseCase;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.SearchProtectedApplicationsUseCase;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.impl.RegisterProtectedApplicationUseCaseImpl;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.impl.SearchProtectedApplicationsUseCaseImpl;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.audit.InMemoryAuditAdapter;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.entity.ProtectedResourceEntity;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.repository.InMemoryProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.transaction.SnapshotCapable;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.transaction.SnapshotReactiveTransactionAdapter;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import co.edu.uco.seguridad.shared.event.DomainEventPublisher;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.ReactiveTransactionPort;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * La única clase consciente de Spring en el módulo. Cablea los puertos secundarios a sus adaptadores
 * y ensambla casos de uso e interactores.
 */
@Configuration
public class ResourcesConfiguration {

    @Bean
    InMemoryProtectedResourceRepository protectedResourceRepository() {
        return new InMemoryProtectedResourceRepository();
    }

    @Bean
    ReactiveTransactionPort reactiveTransactionPort(
            SnapshotCapable<Map<String, ProtectedResourceEntity>> snapshotCapable) {
        return new SnapshotReactiveTransactionAdapter(snapshotCapable);
    }

    @Bean
    InMemoryAuditAdapter protectedResourceAuditListener() {
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
            ProtectedResourceMustBelongToApplicationTenantRule resourceMustBelongToApplicationTenant,
            ProtectedResourceMustBeUniqueRule resourceMustBeUnique) {
        return new RegisterProtectedApplicationRulesValidatorImpl(
                resourceMustBelongToApplicationTenant, resourceMustBeUnique);
    }

    @Bean
    SearchProtectedApplicationsRulesValidator searchProtectedApplicationsRulesValidator(
            TenantMustBeActiveRule tenantMustBeActive) {
        return new SearchProtectedApplicationsRulesValidatorImpl(tenantMustBeActive);
    }

    @Bean
    RegisterProtectedApplicationUseCase registerProtectedApplicationUseCase(
            RegisterApplicationInteractor registerApplicationInteractor,
            RemoveApplicationInteractor removeApplicationInteractor,
            RegisterProtectedApplicationRulesValidator rules,
            ProtectedResourceRepository resources,
            DomainEventPublisher events,
            ReactiveTransactionPort transaction,
            IdentifierGenerator identifiers,
            TimeProvider time) {
        return new RegisterProtectedApplicationUseCaseImpl(
                registerApplicationInteractor,
                removeApplicationInteractor,
                rules,
                resources,
                events,
                transaction,
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
