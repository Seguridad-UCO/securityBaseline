package co.edu.uco.seguridad.pdp.profiles.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.*;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.impl.*;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.*;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.impl.*;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.ProfileMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.ProfileNameMustBeUniqueInScopeRule;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.impl.ProfileMustExistForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.impl.ProfileNameMustBeUniqueInScopeRuleImpl;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.ListProfilesInteractor;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.impl.ListProfilesInteractorImpl;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.secondary.persistence.repository.SurrealProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.secondary.persistence.schema.SurrealProfileSchemaInitializer;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleMustExistForTenantValidator;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La única clase consciente de Spring del módulo.
 */
@Configuration
public class ProfilesConfiguration {

    @Bean
    ProfileRepository profileRepository(SurrealDbClient client) {
        return new SurrealProfileRepository(client);
    }

    @Bean
    ApplicationDeletionDependencyValidator profileApplicationDeletionDependencyValidator(ProfileRepository repository) {
        return new ApplicationDeletionDependencyValidatorImpl(repository);
    }

    @Bean
    RoleDeletionDependencyValidator profileRoleDeletionDependencyValidator(ProfileRepository repository) {
        return new RoleDeletionDependencyValidatorImpl(repository);
    }

    @Bean
    ApplicationRunner profileSchemaInitializer(SurrealDbClient client) {
        return new SurrealProfileSchemaInitializer(client);
    }

    @Bean
    ProfileNameMustBeUniqueInScopeRule profileNameMustBeUniqueInScopeRule() {
        return new ProfileNameMustBeUniqueInScopeRuleImpl();
    }

    @Bean
    ProfileMustExistForTenantRule profileMustExistForTenantRule() {
        return new ProfileMustExistForTenantRuleImpl();
    }

    @Bean
    DefineProfileRulesValidator defineProfileRulesValidator(ApplicationMustExistForTenantValidator applicationMustExist,
                                                            ProfileNameMustBeUniqueInScopeRule nameMustBeUnique, ProfileRepository repository) {
        return new DefineProfileRulesValidatorImpl(applicationMustExist, nameMustBeUnique, repository);
    }

    @Bean
    AddRoleToProfileRulesValidator addRoleToProfileRulesValidator(ProfileRepository repository,
                                                                  ProfileMustExistForTenantRule profileMustExist, RoleMustExistForTenantValidator roleMustExist) {
        return new AddRoleToProfileRulesValidatorImpl(repository, profileMustExist, roleMustExist);
    }

    @Bean
    ProfileRolesLookupValidator profileRolesLookupValidator(ProfileRepository repository,
                                                            ProfileMustExistForTenantRule mustExist) {
        return new ProfileRolesLookupValidatorImpl(repository, mustExist);
    }

    @Bean
    ProfileNamesLookupValidator profileNamesLookupValidator(ProfileRepository repository) {
        return new ProfileNamesLookupValidatorImpl(repository);
    }

    // HU-019 — administración del catálogo de perfiles: resuelve la aplicación de un perfil para
    // que "authorization" pueda gatear definir/agregar-rol sin conocer ProfileRepository.
    @Bean
    ProfileApplicationLookupValidator profileApplicationLookupValidator(ProfileRepository repository,
                                                                        ProfileMustExistForTenantRule mustExist) {
        return new ProfileApplicationLookupValidatorImpl(repository, mustExist);
    }

    @Bean
    DefineProfileUseCase defineProfileUseCase(DefineProfileRulesValidator rules, ProfileRepository repository,
                                              IdentifierGenerator identifiers, TimeProvider time) {
        return new DefineProfileUseCaseImpl(rules, repository, identifiers, time);
    }

    @Bean
    AddRoleToProfileUseCase addRoleToProfileUseCase(AddRoleToProfileRulesValidator rules, ProfileRepository repository) {
        return new AddRoleToProfileUseCaseImpl(rules, repository);
    }

    @Bean
    RemoveRoleFromProfileUseCase removeRoleFromProfileUseCase(AddRoleToProfileRulesValidator rules,
                                                              ProfileRepository repository) {
        return new RemoveRoleFromProfileUseCaseImpl(rules, repository);
    }

    @Bean
    ListProfilesUseCase listProfilesUseCase(ProfileRepository repository) {
        return new ListProfilesUseCaseImpl(repository);
    }

    @Bean
    CountApplicationProfilesUseCase countApplicationProfilesUseCase(ProfileRepository repository) {
        return new CountApplicationProfilesUseCaseImpl(repository);
    }

    @Bean
    ListApplicationProfilesPageUseCase listApplicationProfilesPageUseCase(ProfileRepository repository) {
        return new ListApplicationProfilesPageUseCaseImpl(repository);
    }

    @Bean
    UpdateProfileUseCase updateProfileUseCase(ProfileRepository repository) {
        return new UpdateProfileUseCaseImpl(repository);
    }

    @Bean
    RemoveProfileUseCase removeProfileUseCase(ProfileRepository repository) {
        return new RemoveProfileUseCaseImpl(repository);
    }

    @Bean
    ListProfilesInteractor listProfilesInteractor(ListProfilesUseCase useCase) {
        return new ListProfilesInteractorImpl(useCase);
    }
}
