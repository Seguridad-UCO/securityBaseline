package co.edu.uco.seguridad.pdp.resources.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.rule.ProtectedResourceMustBeUniqueRule;
import co.edu.uco.seguridad.pdp.resources.domain.rule.ProtectedResourceMustExistRule;
import co.edu.uco.seguridad.pdp.resources.domain.rule.impl.ProtectedResourceMustBeUniqueRuleImpl;
import co.edu.uco.seguridad.pdp.resources.domain.rule.impl.ProtectedResourceMustExistRuleImpl;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceMustExistValidator;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.impl.ProtectedResourceMustExistValidatorImpl;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.RegisterProtectedResourceRulesValidator;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.impl.RegisterProtectedResourceRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.resources.application.usecase.ListProtectedResourcesUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedResourceUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.impl.ListProtectedResourcesUseCaseImpl;
import co.edu.uco.seguridad.pdp.resources.application.usecase.impl.RegisterProtectedResourceUseCaseImpl;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.ListProtectedResourcesInteractor;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.RegisterProtectedResourceInteractor;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.impl.ListProtectedResourcesInteractorImpl;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.impl.RegisterProtectedResourceInteractorImpl;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.audit.InMemoryAuditAdapter;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.repository.SurrealProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.schema.SurrealProtectedResourceSchemaInitializer;
import co.edu.uco.seguridad.shared.event.DomainEventPublisher;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La única clase consciente de Spring en el módulo. Cablea los puertos secundarios a sus adaptadores
 * y ensambla casos de uso.
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
    ProtectedResourceMustBeUniqueRule protectedResourceMustBeUniqueRule() {
        return new ProtectedResourceMustBeUniqueRuleImpl();
    }

    @Bean
    ProtectedResourceMustExistRule protectedResourceMustExistRule() {
        return new ProtectedResourceMustExistRuleImpl();
    }

    @Bean
    ProtectedResourceMustExistValidator protectedResourceMustExistValidator(ProtectedResourceRepository repository,
            ProtectedResourceMustExistRule mustExist) {
        return new ProtectedResourceMustExistValidatorImpl(repository, mustExist);
    }

    @Bean
    RegisterProtectedResourceRulesValidator registerProtectedResourceRulesValidator(
            ProtectedResourceMustBeUniqueRule mustBeUnique, ProtectedResourceRepository repository) {
        return new RegisterProtectedResourceRulesValidatorImpl(mustBeUnique, repository);
    }

    @Bean
    RegisterProtectedResourceUseCase registerProtectedResourceUseCase(ApplicationMustExistForTenantValidator applicationMustExist,
            RegisterProtectedResourceRulesValidator rules, ProtectedResourceRepository resources,
            DomainEventPublisher events, IdentifierGenerator identifiers, TimeProvider time) {
        return new RegisterProtectedResourceUseCaseImpl(applicationMustExist, rules, resources, events, identifiers, time);
    }

    @Bean
    ListProtectedResourcesUseCase listProtectedResourcesUseCase(ProtectedResourceRepository resources) {
        return new ListProtectedResourcesUseCaseImpl(resources);
    }

    @Bean
    RegisterProtectedResourceInteractor registerProtectedResourceInteractor(RegisterProtectedResourceUseCase useCase) {
        return new RegisterProtectedResourceInteractorImpl(useCase);
    }

    @Bean
    ListProtectedResourcesInteractor listProtectedResourcesInteractor(ListProtectedResourcesUseCase useCase) {
        return new ListProtectedResourcesInteractorImpl(useCase);
    }
}
