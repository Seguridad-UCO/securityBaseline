package co.edu.uco.seguridad.pdp.resources.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.port.secondary.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.resources.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.application.rule.ProtectedResourceMustBeUniqueRule;
import co.edu.uco.seguridad.pdp.resources.application.rule.impl.ProtectedResourceMustBeUniqueRuleImpl;
import co.edu.uco.seguridad.pdp.resources.application.rulesvalidator.RegisterProtectedResourceRulesValidator;
import co.edu.uco.seguridad.pdp.resources.application.rulesvalidator.impl.RegisterProtectedResourceRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.resources.application.usecase.ListProtectedResourcesUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedResourceUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.impl.ListProtectedResourcesUseCaseImpl;
import co.edu.uco.seguridad.pdp.resources.application.usecase.impl.RegisterProtectedResourceUseCaseImpl;
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
    ProtectedResourceMustBeUniqueRule protectedResourceMustBeUniqueRule(ProtectedResourceRepository repository) {
        return new ProtectedResourceMustBeUniqueRuleImpl(repository);
    }

    @Bean
    RegisterProtectedResourceRulesValidator registerProtectedResourceRulesValidator(
            ProtectedResourceMustBeUniqueRule mustBeUnique) {
        return new RegisterProtectedResourceRulesValidatorImpl(mustBeUnique);
    }

    @Bean
    RegisterProtectedResourceUseCase registerProtectedResourceUseCase(ApplicationRepository applications,
            RegisterProtectedResourceRulesValidator rules, ProtectedResourceRepository resources,
            DomainEventPublisher events, IdentifierGenerator identifiers, TimeProvider time) {
        return new RegisterProtectedResourceUseCaseImpl(applications, rules, resources, events, identifiers, time);
    }

    @Bean
    ListProtectedResourcesUseCase listProtectedResourcesUseCase(ProtectedResourceRepository resources) {
        return new ListProtectedResourcesUseCaseImpl(resources);
    }
}
