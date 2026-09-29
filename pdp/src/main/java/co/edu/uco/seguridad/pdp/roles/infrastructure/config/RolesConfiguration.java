package co.edu.uco.seguridad.pdp.roles.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.*;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl.*;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.application.usecase.*;
import co.edu.uco.seguridad.pdp.roles.application.usecase.impl.*;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleNameMustBeUniqueInScopeRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleScopeMustCoverApplicationRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleScopeMustCoverResourceRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.impl.RoleMustExistForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.roles.domain.rule.impl.RoleNameMustBeUniqueInScopeRuleImpl;
import co.edu.uco.seguridad.pdp.roles.domain.rule.impl.RoleScopeMustCoverApplicationRuleImpl;
import co.edu.uco.seguridad.pdp.roles.domain.rule.impl.RoleScopeMustCoverResourceRuleImpl;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.ListRolesInteractor;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.impl.ListRolesInteractorImpl;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.repository.SurrealRoleRepository;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.schema.SurrealRoleSchemaInitializer;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La única clase consciente de Spring del módulo. Cada regla es un bean para poder sustituirla sin tocar el validador.
 */
@Configuration
public class RolesConfiguration {

    @Bean
    RoleRepository roleRepository(SurrealDbClient client) {
        return new SurrealRoleRepository(client);
    }

    @Bean
    ApplicationDeletionDependencyValidator roleApplicationDeletionDependencyValidator(RoleRepository repository) {
        return new ApplicationDeletionDependencyValidatorImpl(repository);
    }

    @Bean
    ResourceDeletionDependencyValidator resourceDeletionDependencyValidator(RoleRepository repository) {
        return new ResourceDeletionDependencyValidatorImpl(repository);
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
    RevokeResourceFromRoleUseCase revokeResourceFromRoleUseCase(GrantResourceRulesValidator rules,
                                                                RoleRepository repository) {
        return new RevokeResourceFromRoleUseCaseImpl(rules, repository);
    }

    @Bean
    ListRolesUseCase listRolesUseCase(RoleRepository repository) {
        return new ListRolesUseCaseImpl(repository);
    }

    @Bean
    CountApplicationRolesUseCase countApplicationRolesUseCase(RoleRepository repository) {
        return new CountApplicationRolesUseCaseImpl(repository);
    }

    @Bean
    ListApplicationRolesPageUseCase listApplicationRolesPageUseCase(RoleRepository repository) {
        return new ListApplicationRolesPageUseCaseImpl(repository);
    }

    @Bean
    UpdateRoleUseCase updateRoleUseCase(RoleRepository repository) {
        return new UpdateRoleUseCaseImpl(repository);
    }

    @Bean
    RemoveRoleUseCase removeRoleUseCase(RoleRepository repository) {
        return new RemoveRoleUseCaseImpl(repository);
    }

    // HU-016 — defineRoleInteractor/grantResourceToRoleInteractor se retiraron de aquí: las
    // escrituras se exponen ahora desde RoleAdministrationController (authorization), que gatea
    // contra el mecanismo de administración por aplicación. DefineRoleUseCase/GrantResourceToRoleUseCase
    // siguen aquí, sin cambios: authorization los consume vía "roles :: usecase".
    @Bean
    ListRolesInteractor listRolesInteractor(ListRolesUseCase useCase) {
        return new ListRolesInteractorImpl(useCase);
    }

    @Bean
    RoleScopeMustCoverApplicationRule roleScopeMustCoverApplicationRule() {
        return new RoleScopeMustCoverApplicationRuleImpl();
    }

    @Bean
    RoleScopeMustCoverApplicationValidator roleScopeMustCoverApplicationValidator(RoleRepository repository,
                                                                                  RoleMustExistForTenantRule roleMustExist, RoleScopeMustCoverApplicationRule coverageRule) {
        return new RoleScopeMustCoverApplicationValidatorImpl(repository, roleMustExist, coverageRule);
    }

    @Bean
    RoleNamesLookupValidator roleNamesLookupValidator(RoleRepository repository) {
        return new RoleNamesLookupValidatorImpl(repository);
    }

    @Bean
    RoleResourcesLookupValidator roleResourcesLookupValidator(RoleRepository repository) {
        return new RoleResourcesLookupValidatorImpl(repository);
    }

    // HU-015 — publicado para que `assignments` encuentre el rol ADMIN de una aplicación sin
    // consultar RoleRepository directamente (sb-arquitectura, regla invariante 11).
    @Bean
    RoleLookupByNameInScopeValidator roleLookupByNameInScopeValidator(RoleRepository repository) {
        return new RoleLookupByNameInScopeValidatorImpl(repository);
    }

    // HU-011 — publicado para que `profiles` compruebe si un rol existe para el inquilino.
    @Bean
    RoleMustExistForTenantValidator roleMustExistForTenantValidator(RoleRepository repository,
                                                                    RoleMustExistForTenantRule mustExist) {
        return new RoleMustExistForTenantValidatorImpl(repository, mustExist);
    }

    // HU-016 — publicado para que `authorization` resuelva contra qué aplicación gatear una
    // concesión de recurso, sin consultar RoleRepository directamente (sb-arquitectura, regla 11).
    @Bean
    RoleApplicationLookupValidator roleApplicationLookupValidator(RoleRepository repository,
                                                                  RoleMustExistForTenantRule mustExist) {
        return new RoleApplicationLookupValidatorImpl(repository, mustExist);
    }
}
