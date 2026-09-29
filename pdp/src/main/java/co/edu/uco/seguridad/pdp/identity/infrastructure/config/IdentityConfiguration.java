package co.edu.uco.seguridad.pdp.identity.infrastructure.config;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.UserMustExistValidator;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.impl.SubjectUserIdLookupValidatorImpl;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.impl.UserMustExistValidatorImpl;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.application.usecase.AssignTenantUseCase;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ListUsersUseCase;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ProvisionIdentityUseCase;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ResolveExternalIdentityUseCase;
import co.edu.uco.seguridad.pdp.identity.application.usecase.SearchUsersPageUseCase;
import co.edu.uco.seguridad.pdp.identity.application.usecase.impl.AssignTenantUseCaseImpl;
import co.edu.uco.seguridad.pdp.identity.application.usecase.impl.ListUsersUseCaseImpl;
import co.edu.uco.seguridad.pdp.identity.application.usecase.impl.ProvisionIdentityUseCaseImpl;
import co.edu.uco.seguridad.pdp.identity.application.usecase.impl.ResolveExternalIdentityUseCaseImpl;
import co.edu.uco.seguridad.pdp.identity.application.usecase.impl.SearchUsersPageUseCaseImpl;
import co.edu.uco.seguridad.pdp.identity.domain.rule.UserMustExistRule;
import co.edu.uco.seguridad.pdp.identity.domain.rule.impl.UserMustExistRuleImpl;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.interactor.AssignTenantInteractor;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.interactor.ListUsersInteractor;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.interactor.impl.AssignTenantInteractorImpl;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.interactor.impl.ListUsersInteractorImpl;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.repository.SurrealSecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.schema.SurrealIdentitySchemaInitializer;
import co.edu.uco.seguridad.pdp.identity.infrastructure.properties.IdentityProvisioningProperties;
import co.edu.uco.seguridad.pdp.tenants.application.rule.validator.TenantMustBeActiveValidator;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La única clase consciente de Spring en el módulo.
 */
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
    ResolveExternalIdentityUseCase resolveExternalIdentityUseCase(SecurityUserRepository repository) {
        return new ResolveExternalIdentityUseCaseImpl(repository);
    }

    @Bean
    ListUsersUseCase listUsersUseCase(SecurityUserRepository repository) {
        return new ListUsersUseCaseImpl(repository);
    }

    @Bean
    SearchUsersPageUseCase searchUsersPageUseCase(SecurityUserRepository repository) {
        return new SearchUsersPageUseCaseImpl(repository);
    }

    @Bean
    AssignTenantUseCase assignTenantUseCase(TenantMustBeActiveValidator tenantMustBeActive,
                                            SecurityUserRepository repository, UserMustExistRule userMustExist) {
        return new AssignTenantUseCaseImpl(tenantMustBeActive, userMustExist, repository);
    }

    @Bean
    ListUsersInteractor listUsersInteractor(ListUsersUseCase useCase) {
        return new ListUsersInteractorImpl(useCase);
    }

    @Bean
    AssignTenantInteractor assignTenantInteractor(AssignTenantUseCase useCase) {
        return new AssignTenantInteractorImpl(useCase);
    }

    @Bean
    UserMustExistRule userMustExistRule() {
        return new UserMustExistRuleImpl();
    }

    @Bean
    UserMustExistValidator userMustExistValidator(SecurityUserRepository repository, UserMustExistRule mustExist) {
        return new UserMustExistValidatorImpl(repository, mustExist);
    }

    // HU-015 (enmienda §14) — resuelve el UserId de quien llama cuando el principal no lo trae ya
    // resuelto (JWT crudo, sin pasar por LocalUserPrincipal).
    @Bean
    SubjectUserIdLookupValidator subjectUserIdLookupValidator(SecurityUserRepository repository) {
        return new SubjectUserIdLookupValidatorImpl(repository);
    }
}
