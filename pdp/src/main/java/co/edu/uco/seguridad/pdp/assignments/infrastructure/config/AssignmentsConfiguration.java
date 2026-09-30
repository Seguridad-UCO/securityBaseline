package co.edu.uco.seguridad.pdp.assignments.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.*;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl.*;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.*;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.impl.*;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.*;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.impl.*;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.AssignApplicationAdministratorInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.ListAssignmentsInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.ListProfileAssignmentsInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.RegisterApplicationWithFirstAdministratorInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl.AssignApplicationAdministratorInteractorImpl;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl.ListAssignmentsInteractorImpl;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl.ListProfileAssignmentsInteractorImpl;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl.RegisterApplicationWithFirstAdministratorInteractorImpl;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.repository.SurrealAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.repository.SurrealProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.schema.SurrealAssignmentSchemaInitializer;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.schema.SurrealProfileAssignmentSchemaInitializer;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.UserMustExistValidator;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileNamesLookupValidator;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileRolesLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleLookupByNameInScopeValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleNamesLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleResourcesLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleScopeMustCoverApplicationValidator;
import co.edu.uco.seguridad.pdp.roles.application.usecase.DefineRoleUseCase;
import co.edu.uco.seguridad.shared.cache.DistributedCachePort;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La única clase consciente de Spring del módulo.
 */
@Configuration
public class AssignmentsConfiguration {

    @Bean
    AssignmentRepository assignmentRepository(SurrealDbClient client) {
        return new SurrealAssignmentRepository(client);
    }

    @Bean
    ApplicationDeletionDependencyValidator assignmentApplicationDeletionDependencyValidator(
            AssignmentRepository assignments, ProfileAssignmentRepository profileAssignments) {
        return new ApplicationDeletionDependencyValidatorImpl(assignments, profileAssignments);
    }

    @Bean
    RoleDeletionDependencyValidator roleDeletionDependencyValidator(AssignmentRepository repository, TimeProvider time) {
        return new RoleDeletionDependencyValidatorImpl(repository, time);
    }

    @Bean
    ApplicationRunner assignmentSchemaInitializer(SurrealDbClient client) {
        return new SurrealAssignmentSchemaInitializer(client);
    }

    @Bean
    AssignmentMustNotDuplicateActiveRule assignmentMustNotDuplicateActiveRule() {
        return new AssignmentMustNotDuplicateActiveRuleImpl();
    }

    @Bean
    AssignmentMustExistForTenantRule assignmentMustExistForTenantRule() {
        return new AssignmentMustExistForTenantRuleImpl();
    }

    @Bean
    AssignRoleRulesValidator assignRoleRulesValidator(UserMustExistValidator userMustExist,
                                                      ApplicationMustExistForTenantValidator applicationMustExist,
                                                      RoleScopeMustCoverApplicationValidator roleScopeMustCoverApplication,
                                                      AssignmentMustNotDuplicateActiveRule mustNotDuplicate, AssignmentRepository repository, TimeProvider time) {
        return new AssignRoleRulesValidatorImpl(userMustExist, applicationMustExist, roleScopeMustCoverApplication,
                mustNotDuplicate, repository, time);
    }

    @Bean
    RevokeAssignmentRulesValidator revokeAssignmentRulesValidator(AssignmentRepository repository,
                                                                  AssignmentMustExistForTenantRule mustExist) {
        return new RevokeAssignmentRulesValidatorImpl(repository, mustExist);
    }

    // HU-018 — administración del catálogo de asignaciones: resuelve la aplicación de una asignación
    // para que "authorization" pueda gatear su revocación sin conocer AssignmentRepository.
    @Bean
    AssignmentApplicationLookupValidator assignmentApplicationLookupValidator(AssignmentRepository repository,
                                                                              AssignmentMustExistForTenantRule mustExist) {
        return new AssignmentApplicationLookupValidatorImpl(repository, mustExist);
    }

    @Bean
    AssignRoleUseCase assignRoleUseCase(AssignRoleRulesValidator rules, AssignmentRepository repository,
                                        IdentifierGenerator identifiers, TimeProvider time, DistributedCachePort cache) {
        return new AssignRoleUseCaseImpl(rules, repository, identifiers, time, cache);
    }

    @Bean
    RevokeAssignmentUseCase revokeAssignmentUseCase(RevokeAssignmentRulesValidator rules, AssignmentRepository repository,
                                                    TimeProvider time, DistributedCachePort cache) {
        return new RevokeAssignmentUseCaseImpl(rules, repository, time, cache);
    }

    @Bean
    ListAssignmentsUseCase listAssignmentsUseCase(AssignmentRepository repository) {
        return new ListAssignmentsUseCaseImpl(repository);
    }

    @Bean
    ListApplicationRoleAssignmentsPageUseCase listApplicationRoleAssignmentsPageUseCase(AssignmentRepository repository) {
        return new ListApplicationRoleAssignmentsPageUseCaseImpl(repository);
    }

    @Bean
    ListApplicationAdministratorsPageUseCase listApplicationAdministratorsPageUseCase(
            RoleLookupByNameInScopeValidator roles, AssignmentRepository repository, TimeProvider time) {
        return new ListApplicationAdministratorsPageUseCaseImpl(roles, repository, time);
    }

    @Bean
    ReadApplicationAssignmentCountsUseCase readApplicationAssignmentCountsUseCase(
            AssignmentRepository assignments, ProfileAssignmentRepository profileAssignments,
            RoleLookupByNameInScopeValidator roles, TimeProvider time) {
        return new ReadApplicationAssignmentCountsUseCaseImpl(assignments, profileAssignments, roles, time);
    }

    @Bean
    ResolveActiveRolesUseCase resolveActiveRolesUseCase(AssignmentRepository repository, TimeProvider time,
                                                        DistributedCachePort cache) {
        return new ResolveActiveRolesUseCaseImpl(repository, time, cache);
    }

    @Bean
    ResolveAuthorizationSubjectFactsUseCase resolveAuthorizationSubjectFactsUseCase(AssignmentRepository assignments,
                                                                                    ProfileAssignmentRepository profiles, ProfileRolesLookupValidator profileRoles, ProfileNamesLookupValidator profileNames,
                                                                                    RoleNamesLookupValidator roleNames, RoleResourcesLookupValidator resources, TimeProvider time) {
        return new ResolveAuthorizationSubjectFactsUseCaseImpl(assignments, profiles, profileRoles, profileNames, roleNames, resources, time);
    }

    @Bean
    ListAssignmentsInteractor listAssignmentsInteractor(ListAssignmentsUseCase useCase) {
        return new ListAssignmentsInteractorImpl(useCase);
    }

    // HU-011 — asignación de perfiles: materializa una Assignment por rol reutilizando AssignRoleUseCase.
    @Bean
    ProfileAssignmentRepository profileAssignmentRepository(SurrealDbClient client) {
        return new SurrealProfileAssignmentRepository(client);
    }

    @Bean
    ListApplicationProfileAssignmentsPageUseCase listApplicationProfileAssignmentsPageUseCase(ProfileAssignmentRepository repository) {
        return new ListApplicationProfileAssignmentsPageUseCaseImpl(repository);
    }

    @Bean
    ProfileDeletionDependencyValidator profileDeletionDependencyValidator(ProfileAssignmentRepository repository,
                                                                          TimeProvider time) {
        return new ProfileDeletionDependencyValidatorImpl(repository, time);
    }

    @Bean
    ApplicationRunner profileAssignmentSchemaInitializer(SurrealDbClient client) {
        return new SurrealProfileAssignmentSchemaInitializer(client);
    }

    @Bean
    ProfileAssignmentMustNotDuplicateActiveRule profileAssignmentMustNotDuplicateActiveRule() {
        return new ProfileAssignmentMustNotDuplicateActiveRuleImpl();
    }

    @Bean
    ProfileAssignmentMustExistForTenantRule profileAssignmentMustExistForTenantRule() {
        return new ProfileAssignmentMustExistForTenantRuleImpl();
    }

    @Bean
    AssignProfileRulesValidator assignProfileRulesValidator(ProfileRolesLookupValidator profileRolesLookup,
                                                            ProfileAssignmentRepository repository, ProfileAssignmentMustNotDuplicateActiveRule mustNotDuplicate,
                                                            TimeProvider time) {
        return new AssignProfileRulesValidatorImpl(profileRolesLookup, repository, mustNotDuplicate, time);
    }

    @Bean
    RevokeProfileAssignmentRulesValidator revokeProfileAssignmentRulesValidator(ProfileAssignmentRepository repository,
                                                                                ProfileAssignmentMustExistForTenantRule mustExist) {
        return new RevokeProfileAssignmentRulesValidatorImpl(repository, mustExist);
    }

    // HU-019 — administración del catálogo de perfiles: resuelve la aplicación de una asignación de
    // perfil para que "authorization" pueda gatear su revocación.
    @Bean
    ProfileAssignmentApplicationLookupValidator profileAssignmentApplicationLookupValidator(
            ProfileAssignmentRepository repository, ProfileAssignmentMustExistForTenantRule mustExist) {
        return new ProfileAssignmentApplicationLookupValidatorImpl(repository, mustExist);
    }

    @Bean
    AssignProfileUseCase assignProfileUseCase(AssignProfileRulesValidator rules, AssignRoleUseCase assignRoleUseCase,
                                              ProfileAssignmentRepository repository, IdentifierGenerator identifiers, TimeProvider time) {
        return new AssignProfileUseCaseImpl(rules, assignRoleUseCase, repository, identifiers, time);
    }

    @Bean
    RevokeProfileAssignmentUseCase revokeProfileAssignmentUseCase(RevokeProfileAssignmentRulesValidator rules,
                                                                  RevokeAssignmentUseCase revokeAssignmentUseCase, ProfileAssignmentRepository repository, TimeProvider time) {
        return new RevokeProfileAssignmentUseCaseImpl(rules, revokeAssignmentUseCase, repository, time);
    }

    // Consulta de asignaciones de perfil: no existía ningún GET (el frontend las guardaba solo en
    // memoria de sesión del navegador) — mismo patrón que ListAssignmentsUseCase/Interactor.
    @Bean
    ListProfileAssignmentsUseCase listProfileAssignmentsUseCase(ProfileAssignmentRepository repository) {
        return new ListProfileAssignmentsUseCaseImpl(repository);
    }

    @Bean
    ListProfileAssignmentsInteractor listProfileAssignmentsInteractor(ListProfileAssignmentsUseCase useCase) {
        return new ListProfileAssignmentsInteractorImpl(useCase);
    }

    // HU-015 — administración del catálogo de aplicaciones: alta del primer administrador y backfill
    // manual. Viven aquí, no en "applications" ni en "roles": es el único módulo que ya depende de
    // los dos (ver PLAN-HU-015.md §0).
    @Bean
    RegisterApplicationWithFirstAdministratorUseCase registerApplicationWithFirstAdministratorUseCase(
            RegisterApplicationUseCase registerApplication, DefineRoleUseCase defineRole, AssignRoleUseCase assignRole) {
        return new RegisterApplicationWithFirstAdministratorUseCaseImpl(registerApplication, defineRole, assignRole);
    }

    @Bean
    RegisterApplicationWithFirstAdministratorInteractor registerApplicationWithFirstAdministratorInteractor(
            RegisterApplicationWithFirstAdministratorUseCase useCase, SubjectUserIdLookupValidator subjectUserIdLookup) {
        return new RegisterApplicationWithFirstAdministratorInteractorImpl(useCase, subjectUserIdLookup);
    }

    @Bean
    AssignApplicationAdministratorUseCase assignApplicationAdministratorUseCase(
            RoleLookupByNameInScopeValidator roleLookup, DefineRoleUseCase defineRole, AssignRoleUseCase assignRole) {
        return new AssignApplicationAdministratorUseCaseImpl(roleLookup, defineRole, assignRole);
    }

    @Bean
    AssignApplicationAdministratorInteractor assignApplicationAdministratorInteractor(
            ApplicationOwnerLookupValidator ownerLookup, AssignApplicationAdministratorUseCase useCase) {
        return new AssignApplicationAdministratorInteractorImpl(ownerLookup, useCase);
    }

    // HU-020 — autoservicio de administradores. Beans sin cablear a un controller todavía (FASE 5
    // del planificador): el adaptador web queda para 2-tester-spec, igual que en HU-018/HU-019.
    @Bean
    LastAdministratorMustNotBeRevokedRule lastAdministratorMustNotBeRevokedRule() {
        return new LastAdministratorMustNotBeRevokedRuleImpl();
    }

    @Bean
    RemoveApplicationAdministratorUseCase removeApplicationAdministratorUseCase(
            RoleLookupByNameInScopeValidator roleLookup, AssignmentRepository repository,
            LastAdministratorMustNotBeRevokedRule mustNotBeLastAdministrator, RevokeAssignmentUseCase revokeAssignment,
            TimeProvider time, TokenRevocationPort revocation) {
        return new RemoveApplicationAdministratorUseCaseImpl(roleLookup, repository, mustNotBeLastAdministrator,
                revokeAssignment, time, revocation);
    }

    @Bean
    ListApplicationAdministratorsUseCase listApplicationAdministratorsUseCase(
            RoleLookupByNameInScopeValidator roleLookup, AssignmentRepository repository, TimeProvider time) {
        return new ListApplicationAdministratorsUseCaseImpl(roleLookup, repository, time);
    }
}
