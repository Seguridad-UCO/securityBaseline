package co.edu.uco.seguridad.pdp.roles.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.DefineRoleRulesValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.GrantResourceRulesValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl.DefineRoleRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl.GrantResourceRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.application.usecase.DefineRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.application.usecase.GrantResourceToRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.application.usecase.ListRolesUseCase;
import co.edu.uco.seguridad.pdp.roles.application.usecase.impl.DefineRoleUseCaseImpl;
import co.edu.uco.seguridad.pdp.roles.application.usecase.impl.GrantResourceToRoleUseCaseImpl;
import co.edu.uco.seguridad.pdp.roles.application.usecase.impl.ListRolesUseCaseImpl;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleNameMustBeUniqueInScopeRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleScopeMustCoverResourceRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.impl.RoleMustExistForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.roles.domain.rule.impl.RoleNameMustBeUniqueInScopeRuleImpl;
import co.edu.uco.seguridad.pdp.roles.domain.rule.impl.RoleScopeMustCoverResourceRuleImpl;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.DefineRoleInteractor;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.GrantResourceToRoleInteractor;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.ListRolesInteractor;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.impl.DefineRoleInteractorImpl;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.impl.GrantResourceToRoleInteractorImpl;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.impl.ListRolesInteractorImpl;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.repository.SurrealRoleRepository;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.schema.SurrealRoleSchemaInitializer;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** La única clase consciente de Spring del módulo. Cada regla es un bean para poder sustituirla sin tocar el validador. */
@Configuration
public class RolesConfiguration {

    @Bean
    RoleRepository roleRepository(SurrealDbClient client) {
        return new SurrealRoleRepository(client);
    }

    @Bean
    ApplicationRunner roleSchemaInitializer(SurrealDbClient client) {
        return new SurrealRoleSchemaInitializer(client);
    }

    @Bean
    RoleNameMustBeUniqueInScopeRule roleNameMustBeUniqueInScopeRule() {
        return new RoleNameMustBeUniqueInScopeRuleImpl();
    }

    @Bean
    RoleMustExistForTenantRule roleMustExistForTenantRule() {
        return new RoleMustExistForTenantRuleImpl();
    }

    @Bean
    RoleScopeMustCoverResourceRule roleScopeMustCoverResourceRule() {
        return new RoleScopeMustCoverResourceRuleImpl();
    }

    @Bean
    DefineRoleRulesValidator defineRoleRulesValidator(ApplicationMustExistForTenantValidator applicationMustExist,
            RoleNameMustBeUniqueInScopeRule nameMustBeUnique, RoleRepository repository) {
        return new DefineRoleRulesValidatorImpl(applicationMustExist, nameMustBeUnique, repository);
    }

    @Bean
    GrantResourceRulesValidator grantResourceRulesValidator(RoleRepository repository,
            RoleMustExistForTenantRule roleMustExist, ProtectedResourceOwnerLookupValidator resourceOwner,
            ApplicationOwnerLookupValidator applicationOwner, RoleScopeMustCoverResourceRule scopeMustCover) {
        return new GrantResourceRulesValidatorImpl(repository, roleMustExist, resourceOwner, applicationOwner, scopeMustCover);
    }

    @Bean
    DefineRoleUseCase defineRoleUseCase(DefineRoleRulesValidator rules, RoleRepository repository,
            IdentifierGenerator identifiers, TimeProvider time) {
        return new DefineRoleUseCaseImpl(rules, repository, identifiers, time);
    }

    @Bean
    GrantResourceToRoleUseCase grantResourceToRoleUseCase(GrantResourceRulesValidator rules, RoleRepository repository) {
        return new GrantResourceToRoleUseCaseImpl(rules, repository);
    }

    @Bean
    ListRolesUseCase listRolesUseCase(RoleRepository repository) {
        return new ListRolesUseCaseImpl(repository);
    }

    @Bean
    DefineRoleInteractor defineRoleInteractor(DefineRoleUseCase useCase) {
        return new DefineRoleInteractorImpl(useCase);
    }

    @Bean
    GrantResourceToRoleInteractor grantResourceToRoleInteractor(GrantResourceToRoleUseCase useCase) {
        return new GrantResourceToRoleInteractorImpl(useCase);
    }

    @Bean
    ListRolesInteractor listRolesInteractor(ListRolesUseCase useCase) {
        return new ListRolesInteractorImpl(useCase);
    }
}
