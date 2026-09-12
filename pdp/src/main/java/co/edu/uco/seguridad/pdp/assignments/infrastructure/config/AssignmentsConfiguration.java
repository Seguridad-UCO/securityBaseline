package co.edu.uco.seguridad.pdp.assignments.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.AssignRoleRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.RevokeAssignmentRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl.AssignRoleRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl.RevokeAssignmentRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignRoleUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListAssignmentsUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveActiveRolesUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeAssignmentUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.impl.AssignRoleUseCaseImpl;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.impl.ListAssignmentsUseCaseImpl;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.impl.ResolveActiveRolesUseCaseImpl;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.impl.RevokeAssignmentUseCaseImpl;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.AssignmentMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.AssignmentMustNotDuplicateActiveRule;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.impl.AssignmentMustExistForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.impl.AssignmentMustNotDuplicateActiveRuleImpl;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.AssignRoleInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.ListAssignmentsInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.RevokeAssignmentInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl.AssignRoleInteractorImpl;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl.ListAssignmentsInteractorImpl;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl.RevokeAssignmentInteractorImpl;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.repository.SurrealAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.schema.SurrealAssignmentSchemaInitializer;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.UserMustExistValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleScopeMustCoverApplicationValidator;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** La única clase consciente de Spring del módulo. */
@Configuration
public class AssignmentsConfiguration {

    @Bean
    AssignmentRepository assignmentRepository(SurrealDbClient client) {
        return new SurrealAssignmentRepository(client);
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

    @Bean
    AssignRoleUseCase assignRoleUseCase(AssignRoleRulesValidator rules, AssignmentRepository repository,
            IdentifierGenerator identifiers, TimeProvider time) {
        return new AssignRoleUseCaseImpl(rules, repository, identifiers, time);
    }

    @Bean
    RevokeAssignmentUseCase revokeAssignmentUseCase(RevokeAssignmentRulesValidator rules, AssignmentRepository repository,
            TimeProvider time) {
        return new RevokeAssignmentUseCaseImpl(rules, repository, time);
    }

    @Bean
    ListAssignmentsUseCase listAssignmentsUseCase(AssignmentRepository repository) {
        return new ListAssignmentsUseCaseImpl(repository);
    }

    @Bean
    ResolveActiveRolesUseCase resolveActiveRolesUseCase(AssignmentRepository repository, TimeProvider time) {
        return new ResolveActiveRolesUseCaseImpl(repository, time);
    }

    @Bean
    AssignRoleInteractor assignRoleInteractor(AssignRoleUseCase useCase) {
        return new AssignRoleInteractorImpl(useCase);
    }

    @Bean
    RevokeAssignmentInteractor revokeAssignmentInteractor(RevokeAssignmentUseCase useCase) {
        return new RevokeAssignmentInteractorImpl(useCase);
    }

    @Bean
    ListAssignmentsInteractor listAssignmentsInteractor(ListAssignmentsUseCase useCase) {
        return new ListAssignmentsInteractorImpl(useCase);
    }
}
