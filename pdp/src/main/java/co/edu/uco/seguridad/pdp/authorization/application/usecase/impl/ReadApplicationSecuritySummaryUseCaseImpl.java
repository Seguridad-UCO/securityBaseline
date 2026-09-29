package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationDetailsLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ApplicationAssignmentCountsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ReadApplicationAssignmentCountsUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ReadApplicationSecuritySummaryRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.ApplicationSecuritySummaryResponse;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ReadApplicationSecuritySummaryUseCase;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ApplicationProfileCountRequest;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.CountApplicationProfilesUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.CountApplicationProtectedResourcesUseCase;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.ApplicationRoleCountRequest;
import co.edu.uco.seguridad.pdp.roles.application.usecase.CountApplicationRolesUseCase;
import reactor.core.publisher.Mono;

/**
 * The administrative gate is evaluated before delegating every count to its owning module.
 * The authorization module never accesses another module's persistence port directly.
 */
public final class ReadApplicationSecuritySummaryUseCaseImpl implements ReadApplicationSecuritySummaryUseCase {
    private final PrincipalMustBeApplicationAdministratorValidator gate;
    private final ApplicationDetailsLookupValidator applications;
    private final CountApplicationProtectedResourcesUseCase resources;
    private final CountApplicationRolesUseCase roles;
    private final CountApplicationProfilesUseCase profiles;
    private final ReadApplicationAssignmentCountsUseCase assignments;

    public ReadApplicationSecuritySummaryUseCaseImpl(
            PrincipalMustBeApplicationAdministratorValidator gate,
            ApplicationDetailsLookupValidator applications,
            CountApplicationProtectedResourcesUseCase resources,
            CountApplicationRolesUseCase roles,
            CountApplicationProfilesUseCase profiles,
            ReadApplicationAssignmentCountsUseCase assignments) {
        this.gate = gate;
        this.applications = applications;
        this.resources = resources;
        this.roles = roles;
        this.profiles = profiles;
        this.assignments = assignments;
    }

    @Override
    public Mono<ApplicationSecuritySummaryResponse> execute(ReadApplicationSecuritySummaryRequest request) {
        var administration = request.administration();
        return gate.execute(administration).then(Mono.defer(() -> Mono.zip(
                applications.execute(administration.applicationId()),
                resources.execute(administration.applicationId()),
                roles.execute(new ApplicationRoleCountRequest(administration.tenantId(), administration.applicationId())),
                profiles.execute(new ApplicationProfileCountRequest(administration.tenantId(), administration.applicationId())),
                assignments.execute(new ApplicationAssignmentCountsRequest(administration.tenantId(), administration.applicationId())))
                .map(result -> new ApplicationSecuritySummaryResponse(
                        new ApplicationSecuritySummaryResponse.Application(result.getT1().id().value().toString(),
                                result.getT1().name().value(), result.getT1().description(), result.getT1().baseUrl().value()),
                        new ApplicationSecuritySummaryResponse.Counts(result.getT2(), result.getT3(), result.getT4(),
                                result.getT5().administrators(), result.getT5().roleAssignments(),
                                result.getT5().profileAssignments())))));
    }
}
