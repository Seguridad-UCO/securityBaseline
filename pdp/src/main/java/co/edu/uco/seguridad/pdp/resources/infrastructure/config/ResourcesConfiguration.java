package co.edu.uco.seguridad.pdp.resources.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.rule.ProtectedResourceMustBeUniqueRule;
import co.edu.uco.seguridad.pdp.resources.domain.rule.ProtectedResourceMustExistRule;
import co.edu.uco.seguridad.pdp.resources.domain.rule.impl.ProtectedResourceMustBeUniqueRuleImpl;
import co.edu.uco.seguridad.pdp.resources.domain.rule.impl.ProtectedResourceMustExistRuleImpl;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceMustExistValidator;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceIdLookupValidator;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.impl.ProtectedResourceMustExistValidatorImpl;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.impl.ProtectedResourceOwnerLookupValidatorImpl;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.impl.ProtectedResourceIdLookupValidatorImpl;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.RegisterProtectedResourceRulesValidator;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.impl.RegisterProtectedResourceRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.resources.application.usecase.ListProtectedResourcesUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterApplicationWithInitialResourceUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedResourceUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RemoveProtectedResourceUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.UpdateProtectedResourceUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.impl.ListProtectedResourcesUseCaseImpl;
import co.edu.uco.seguridad.pdp.resources.application.usecase.impl.RegisterApplicationWithInitialResourceUseCaseImpl;
import co.edu.uco.seguridad.pdp.resources.application.usecase.impl.RegisterProtectedResourceUseCaseImpl;
import co.edu.uco.seguridad.pdp.resources.application.usecase.impl.RemoveProtectedResourceUseCaseImpl;
import co.edu.uco.seguridad.pdp.resources.application.usecase.impl.UpdateProtectedResourceUseCaseImpl;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.ListProtectedResourcesInteractor;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.RegisterApplicationWithInitialResourceInteractor;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.impl.ListProtectedResourcesInteractorImpl;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.impl.RegisterApplicationWithInitialResourceInteractorImpl;
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

    // HU-004 — catálogo de roles: resuelve la aplicación dueña a partir solo del ResourceId.
    @Bean
    ProtectedResourceOwnerLookupValidator protectedResourceOwnerLookupValidator(ProtectedResourceRepository repository) {
        return new ProtectedResourceOwnerLookupValidatorImpl(repository);
    }

    @Bean
    ProtectedResourceIdLookupValidator protectedResourceIdLookupValidator(ProtectedResourceRepository repository) {
        return new ProtectedResourceIdLookupValidatorImpl(repository);
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
    UpdateProtectedResourceUseCase updateProtectedResourceUseCase(ProtectedResourceRepository repository) {
        return new UpdateProtectedResourceUseCaseImpl(repository);
    }

    @Bean
    RemoveProtectedResourceUseCase removeProtectedResourceUseCase(ProtectedResourceRepository repository) {
        return new RemoveProtectedResourceUseCaseImpl(repository);
    }

    // HU-017 — registerProtectedResourceInteractor se retiró de aquí: la escritura se expone ahora
    // desde ResourceAdministrationController (authorization), que gatea contra el mecanismo de
    // administración por aplicación. RegisterProtectedResourceUseCase sigue aquí, sin cambios:
    // authorization lo consume vía "resources :: usecase", y la saga de HU-010 lo sigue invocando
    // directo (sin gate, mismo precedente que RegisterApplicationUseCase en HU-015).
    @Bean
    ListProtectedResourcesInteractor listProtectedResourcesInteractor(ListProtectedResourcesUseCase useCase) {
        return new ListProtectedResourcesInteractorImpl(useCase);
    }

    // HU-010 — saga de registro de aplicación con recurso inicial, con compensación explícita.
    @Bean
    RegisterApplicationWithInitialResourceUseCase registerApplicationWithInitialResourceUseCase(
            RegisterApplicationUseCase registerApplication, RegisterProtectedResourceUseCase registerResource,
            RemoveApplicationUseCase removeApplication) {
        return new RegisterApplicationWithInitialResourceUseCaseImpl(registerApplication, registerResource,
                removeApplication);
    }

    @Bean
    RegisterApplicationWithInitialResourceInteractor registerApplicationWithInitialResourceInteractor(
            RegisterApplicationWithInitialResourceUseCase useCase) {
        return new RegisterApplicationWithInitialResourceInteractorImpl(useCase);
    }
}
