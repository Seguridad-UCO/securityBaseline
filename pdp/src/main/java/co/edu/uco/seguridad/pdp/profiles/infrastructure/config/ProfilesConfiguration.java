package co.edu.uco.seguridad.pdp.profiles.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.AddRoleToProfileRulesValidator;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.DefineProfileRulesValidator;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileRolesLookupValidator;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.impl.AddRoleToProfileRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.impl.DefineProfileRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.impl.ProfileRolesLookupValidatorImpl;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.AddRoleToProfileUseCase;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.DefineProfileUseCase;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.ListProfilesUseCase;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.impl.AddRoleToProfileUseCaseImpl;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.impl.DefineProfileUseCaseImpl;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.impl.ListProfilesUseCaseImpl;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.ProfileMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.ProfileNameMustBeUniqueInScopeRule;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.impl.ProfileMustExistForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.impl.ProfileNameMustBeUniqueInScopeRuleImpl;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.AddRoleToProfileInteractor;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.DefineProfileInteractor;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.ListProfilesInteractor;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.impl.AddRoleToProfileInteractorImpl;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.impl.DefineProfileInteractorImpl;
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

/** La única clase consciente de Spring del módulo. */
@Configuration
public class ProfilesConfiguration {

    @Bean
    ProfileRepository profileRepository(SurrealDbClient client) {
        return new SurrealProfileRepository(client);
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
    DefineProfileUseCase defineProfileUseCase(DefineProfileRulesValidator rules, ProfileRepository repository,
            IdentifierGenerator identifiers, TimeProvider time) {
        return new DefineProfileUseCaseImpl(rules, repository, identifiers, time);
    }

    @Bean
    AddRoleToProfileUseCase addRoleToProfileUseCase(AddRoleToProfileRulesValidator rules, ProfileRepository repository) {
        return new AddRoleToProfileUseCaseImpl(rules, repository);
    }

    @Bean
    ListProfilesUseCase listProfilesUseCase(ProfileRepository repository) {
        return new ListProfilesUseCaseImpl(repository);
    }

    @Bean
    DefineProfileInteractor defineProfileInteractor(DefineProfileUseCase useCase) {
        return new DefineProfileInteractorImpl(useCase);
    }

    @Bean
    AddRoleToProfileInteractor addRoleToProfileInteractor(AddRoleToProfileUseCase useCase) {
        return new AddRoleToProfileInteractorImpl(useCase);
    }

    @Bean
    ListProfilesInteractor listProfilesInteractor(ListProfilesUseCase useCase) {
        return new ListProfilesInteractorImpl(useCase);
    }
}
