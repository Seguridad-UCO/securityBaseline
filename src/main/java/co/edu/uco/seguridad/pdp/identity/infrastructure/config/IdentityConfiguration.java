package co.edu.uco.seguridad.pdp.identity.infrastructure.config;

import co.edu.uco.seguridad.pdp.identity.application.port.secondary.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.application.usecase.AssignTenantUseCase;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ListUsersUseCase;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ProvisionIdentityUseCase;
import co.edu.uco.seguridad.pdp.identity.application.usecase.impl.AssignTenantUseCaseImpl;
import co.edu.uco.seguridad.pdp.identity.application.usecase.impl.ListUsersUseCaseImpl;
import co.edu.uco.seguridad.pdp.identity.application.usecase.impl.ProvisionIdentityUseCaseImpl;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.repository.SurrealSecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.schema.SurrealIdentitySchemaInitializer;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.identity.infrastructure.properties.IdentityProvisioningProperties;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** La única clase consciente de Spring en el módulo. */
@Configuration
@EnableConfigurationProperties(IdentityProvisioningProperties.class)
public class IdentityConfiguration {

    @Bean
    SecurityUserRepository securityUserRepository(SurrealDbClient client) {
        return new SurrealSecurityUserRepository(client);
    }

    @Bean
    ApplicationRunner identitySchemaInitializer(SurrealDbClient client) {
        return new SurrealIdentitySchemaInitializer(client);
    }

    @Bean
    ProvisionIdentityUseCase provisionIdentityUseCase(SecurityUserRepository repository,
            IdentityProvisioningProperties properties, IdentifierGenerator identifiers, TimeProvider time) {
        return new ProvisionIdentityUseCaseImpl(repository, new TenantId(properties.defaultTenantId()),
                identifiers, time);
    }

    @Bean
    ListUsersUseCase listUsersUseCase(SecurityUserRepository repository) {
        return new ListUsersUseCaseImpl(repository);
    }

    @Bean
    AssignTenantUseCase assignTenantUseCase(TenantMustBeActiveRule tenantMustBeActive,
            SecurityUserRepository repository) {
        return new AssignTenantUseCaseImpl(tenantMustBeActive, repository);
    }
}
