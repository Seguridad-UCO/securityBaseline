package co.edu.uco.seguridad.pdp.authorization.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveUserApplicationsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveUserApplicationsUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustAdministerAtLeastOneApplicationValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.MfaEvidenceRequiredException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Implementación de {@link PrincipalMustAdministerAtLeastOneApplicationValidator} (HU-036, ADR-036).
 *
 * <p>Compone decisiones que ya toma el gate por aplicación (con su MFA): no decide quién administra.
 * Resuelve las aplicaciones del sujeto en su inquilino, las ordena por id (el mapa que devuelve la
 * resolución no tiene orden, y uno estable hace el corte y la auditoría deterministas), las consulta una
 * a una —nunca en paralelo— y se detiene en el primer permiso. Es <b>fail-closed</b>: cualquier
 * {@link Exception} del gate o de la resolución cuenta como «no autoriza», nunca como un error de
 * servidor; un {@link Error} de la JVM sí se propaga. No asume cómo decide el gate por dentro (si el MFA
 * pasara a ser por aplicación, cortar antes sería un error), así que un sujeto sin permiso cuesta hasta
 * N consultas.</p>
 *
 * <p>El estado de una ejecución (primera MFA exigida, primera aplicación evaluada) vive en
 * {@link Evaluation}, creado dentro de {@code Mono.defer}: el bean es un singleton.</p>
 */
public final class PrincipalMustAdministerAtLeastOneApplicationValidatorImpl
        implements PrincipalMustAdministerAtLeastOneApplicationValidator {

    private final ResolveUserApplicationsUseCase userApplications;
    private final PrincipalMustBeApplicationAdministratorValidator applicationAdministrator;

    public PrincipalMustAdministerAtLeastOneApplicationValidatorImpl(
            ResolveUserApplicationsUseCase userApplications,
            PrincipalMustBeApplicationAdministratorValidator applicationAdministrator) {
        this.userApplications = Objects.requireNonNull(userApplications,
                RequiredArgumentMessages.RESOLVE_USER_APPLICATIONS_USE_CASE);
        this.applicationAdministrator = Objects.requireNonNull(applicationAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
    }

    @Override
    public Mono<ApplicationId> execute(TenantAdministrationRequest input) {
        return Mono.defer(() -> {
            Evaluation evaluation = new Evaluation();
            return orderedApplications(input)
                    .flatMapMany(Flux::fromIterable)
                    .concatMap(application -> evaluate(input, application, evaluation))
                    .next()
                    .switchIfEmpty(Mono.error(() -> evaluation.rejection(input.tenantId())));
        });
    }

    private Mono<List<ApplicationId>> orderedApplications(TenantAdministrationRequest input) {
        return Mono.defer(() -> userApplications.execute(new ResolveUserApplicationsRequest(input.subjectUserId(), input.tenantId())))
                .map(response -> response.assignmentsByApplication().keySet().stream()
                        .sorted(Comparator.comparing(application -> application.value().toString()))
                        .toList())
                .onErrorResume(Exception.class, error -> Mono.error(
                        new NotAuthorizedToAdministerTenantCatalogException(input.tenantId(), Optional.empty())));
    }

    /** Emite la aplicación si el gate la permite; si no, recuerda por qué y no emite nada. */
    private Mono<ApplicationId> evaluate(TenantAdministrationRequest input, ApplicationId application, Evaluation evaluation) {
        return Mono.defer(() -> applicationAdministrator.execute(new AdministrationRequest(input.tenantId(), application,
                        input.subjectUserId(), input.subject(), Set.of(), input.authenticationContext())))
                .thenReturn(application)
                .onErrorResume(Exception.class, error -> {
                    evaluation.rejected(application, error);
                    return Mono.empty();
                });
    }

    /** Lo recordado durante una sola ejecución; las consultas son secuenciales, así que no hay carreras. */
    private static final class Evaluation {

        private Optional<ApplicationId> firstEvaluated = Optional.empty();
        private Optional<MfaEvidenceRequiredException> firstMfaRequirement = Optional.empty();

        void rejected(ApplicationId application, Exception error) {
            firstEvaluated = firstEvaluated.or(() -> Optional.of(application));
            firstMfaRequirement = firstMfaRequirement.or(() -> mfaRequirementOf(error));
        }

        /** Si alguna aplicación exigió MFA se propaga esa misma excepción (código {@code MFA_REQUIRED}). */
        RuntimeException rejection(TenantId tenantId) {
            return firstMfaRequirement
                    .<RuntimeException>map(requirement -> requirement)
                    .orElseGet(() -> new NotAuthorizedToAdministerTenantCatalogException(tenantId, firstEvaluated));
        }

        private static Optional<MfaEvidenceRequiredException> mfaRequirementOf(Exception error) {
            return error instanceof MfaEvidenceRequiredException requirement ? Optional.of(requirement) : Optional.empty();
        }
    }
}
