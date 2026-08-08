package co.edu.uco.seguridad.pdp.recursos.infrastructure.config;

import co.edu.uco.seguridad.pdp.aplicaciones.ApplicationsModuleApi;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.RegisterProtectedApplicationUseCase;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.SearchProtectedApplicationsUseCase;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.RegisterProtectedApplicationInteractor;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.SearchProtectedApplicationsInteractor;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.impl.RegisterProtectedApplicationInteractorImpl;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.impl.SearchProtectedApplicationsInteractorImpl;
import co.edu.uco.seguridad.pdp.recursos.application.port.secondary.AuditPort;
import co.edu.uco.seguridad.pdp.recursos.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.recursos.application.rule.ProtectedResourceMustBeUniqueRule;
import co.edu.uco.seguridad.pdp.recursos.application.rule.ProtectedResourceMustBelongToApplicationTenantRule;
import co.edu.uco.seguridad.pdp.recursos.application.rule.impl.ProtectedResourceMustBeUniqueRuleImpl;
import co.edu.uco.seguridad.pdp.recursos.application.rule.impl.ProtectedResourceMustBelongToApplicationTenantRuleImpl;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.RegisterProtectedApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.SearchProtectedApplicationsRulesValidator;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.impl.RegisterProtectedApplicationRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.impl.SearchProtectedApplicationsRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.impl.RegisterProtectedApplicationUseCaseImpl;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.impl.SearchProtectedApplicationsUseCaseImpl;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.audit.InMemoryAuditAdapter;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.repository.InMemoryProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.transaction.SnapshotReactiveTransactionAdapter;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.ReactiveTransactionPort;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La única clase consciente de Spring en el módulo. Cablea los puertos secundarios a sus adaptadores
 * y ensambla casos de uso e interactores.
 *
 * <p>El repositorio dummy se expone tanto como el puerto como su tipo concreto: el adaptador de transacción
 * necesita la capacidad de instantánea que el puerto deliberadamente no declara. Ese acoplamiento
 * está confinado a este archivo y desaparece con el dummy.</p>
 */
@Configuration
public class ResourcesConfiguration {

    /**
     * Declarado por su tipo concreto porque el adaptador de transacción necesita la capacidad de instantánea
     * que el puerto deliberadamente no expone. Los puntos de inyección que piden el puerto resuelven a este
     * mismo bean, por lo que sigue existiendo exactamente un almacén.
     */
    @Bean
    InMemoryProtectedResourceRepository protectedResourceRepository() {
        return new InMemoryProtectedResourceRepository();
    }

    @Bean
    ReactiveTransactionPort reactiveTransactionPort(InMemoryProtectedResourceRepository repository) {
        return new SnapshotReactiveTransactionAdapter(repository);
    }

    @Bean
    AuditPort auditPort() {
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
            ApplicationsModuleApi applications,
            RegisterProtectedApplicationRulesValidator rules,
            ProtectedResourceRepository resources,
            AuditPort audit,
            ReactiveTransactionPort transaction,
            IdentifierGenerator identifiers,
            TimeProvider time) {
        return new RegisterProtectedApplicationUseCaseImpl(
                applications, rules, resources, audit, transaction, identifiers, time);
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
