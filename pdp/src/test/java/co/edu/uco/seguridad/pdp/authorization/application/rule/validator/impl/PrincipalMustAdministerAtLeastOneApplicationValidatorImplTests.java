package co.edu.uco.seguridad.pdp.authorization.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveUserApplicationsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.UserApplicationAssignmentsResponse;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.UserApplicationAssignmentsResponse.ApplicationAssignments;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveUserApplicationsUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.MfaEvidenceRequiredException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-036, regla R1: el sujeto administra al menos una aplicación de su inquilino. El validador no decide
 * quién administra (eso lo decide el gate por aplicación que recibe, ya decorado con MFA): compone esas
 * decisiones — aplicaciones en orden ascendente de id, una por una, con corte al primer permiso — y es
 * fail-closed ante cualquier error. El estado de cada ejecución (primera MFA exigida, primera aplicación
 * evaluada) es local a ella: el bean es un singleton.
 */
class PrincipalMustAdministerAtLeastOneApplicationValidatorImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final UserId USER = new UserId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
    private static final String SUBJECT = "test-subject";
    private static final AuthenticationContextEvidence EVIDENCE =
            new AuthenticationContextEvidence(Optional.of("urn:mfa:otp"), List.of("otp"));
    private static final TenantAdministrationRequest ACTOR =
            new TenantAdministrationRequest(TENANT, USER, SUBJECT, EVIDENCE);

    // Los UUID se eligen para que el orden ascendente por texto sea APP_A < APP_B < APP_C
    private static final ApplicationId APP_A = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private static final ApplicationId APP_B = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000b2"));
    private static final ApplicationId APP_C = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000c3"));

    private final List<ResolveUserApplicationsRequest> resolutions = new ArrayList<>();
    private final List<AdministrationRequest> gateCalls = new ArrayList<>();

    /** El sujeto tiene asignaciones en {@code applications}, insertadas en el orden dado (el mapa no lo conserva). */
    private ResolveUserApplicationsUseCase assignedTo(ApplicationId... applications) {
        Map<ApplicationId, ApplicationAssignments> byApplication = new LinkedHashMap<>();
        for (ApplicationId application : applications) {
            byApplication.put(application, new ApplicationAssignments(Set.of(), Set.of()));
        }
        return request -> Mono.defer(() -> {
            resolutions.add(request);
            return Mono.just(new UserApplicationAssignmentsResponse(byApplication));
        });
    }

    private PrincipalMustBeApplicationAdministratorValidator gate(
            java.util.function.Function<ApplicationId, Mono<Void>> perApplication) {
        return request -> Mono.defer(() -> {
            gateCalls.add(request);
            return perApplication.apply(request.applicationId());
        });
    }

    private static Mono<Void> denied(ApplicationId application) {
        return Mono.error(new NotAuthorizedToAdministerException(TENANT, application));
    }

    private static PrincipalMustAdministerAtLeastOneApplicationValidatorImpl validator(
            ResolveUserApplicationsUseCase applications, PrincipalMustBeApplicationAdministratorValidator gate) {
        return new PrincipalMustAdministerAtLeastOneApplicationValidatorImpl(applications, gate);
    }

    private List<ApplicationId> consulted() {
        return gateCalls.stream().map(AdministrationRequest::applicationId).toList();
    }

    // ---- C5, C8: el primer permiso gana ------------------------------------------------------------------------

    @Test
    void emits_the_only_application_when_the_gate_permits_it() {
        var validator = validator(assignedTo(APP_A), gate(application -> Mono.empty()));

        StepVerifier.create(validator.execute(ACTOR))
                .expectNext(APP_A)
                .verifyComplete();
        assertThat(consulted()).containsExactly(APP_A);
    }

    @Test
    void stops_at_the_first_permit_and_does_not_consult_the_rest() {
        var validator = validator(assignedTo(APP_C, APP_B, APP_A), gate(application -> Mono.empty()));

        StepVerifier.create(validator.execute(ACTOR))
                .expectNext(APP_A)
                .verifyComplete();
        assertThat(consulted()).containsExactly(APP_A);
    }

    @Test
    void consults_applications_in_ascending_id_order_regardless_of_map_order() {
        var validator = validator(assignedTo(APP_C, APP_A, APP_B), gate(PrincipalMustAdministerAtLeastOneApplicationValidatorImplTests::denied));

        StepVerifier.create(validator.execute(ACTOR))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOfSatisfying(NotAuthorizedToAdministerTenantCatalogException.class,
                                denial -> assertThat(denial.evaluatedApplication()).contains(APP_A)))
                .verify();
        assertThat(consulted()).containsExactly(APP_A, APP_B, APP_C);
    }

    @Test
    void consults_applications_one_at_a_time_and_never_in_parallel() {
        Sinks.Empty<Void> firstAnswer = Sinks.empty();
        var validator = validator(assignedTo(APP_B, APP_A), gate(application -> application.equals(APP_A)
                ? firstAnswer.asMono() : Mono.empty()));

        StepVerifier.create(validator.execute(ACTOR))
                // la segunda aplicación no se consulta mientras la primera siga sin respuesta
                .then(() -> assertThat(consulted()).containsExactly(APP_A))
                .then(() -> firstAnswer.tryEmitError(new NotAuthorizedToAdministerException(TENANT, APP_A)))
                .expectNext(APP_B)
                .verifyComplete();
        assertThat(consulted()).containsExactly(APP_A, APP_B);
    }

    @Test
    void skips_an_application_it_does_not_administer_and_permits_the_next() {
        var validator = validator(assignedTo(APP_A, APP_B, APP_C),
                gate(application -> application.equals(APP_A) ? denied(application) : Mono.empty()));

        StepVerifier.create(validator.execute(ACTOR))
                .expectNext(APP_B)
                .verifyComplete();
        assertThat(consulted()).containsExactly(APP_A, APP_B);
    }

    // ---- C7: fail-closed ------------------------------------------------------------------------------------------

    @Test
    void denies_when_the_subject_has_no_applications_and_never_calls_the_gate() {
        var validator = validator(assignedTo(), gate(application -> Mono.empty()));

        StepVerifier.create(validator.execute(ACTOR))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOfSatisfying(NotAuthorizedToAdministerTenantCatalogException.class,
                                denial -> assertThat(denial.evaluatedApplication()).isEmpty()))
                .verify();
        assertThat(gateCalls).isEmpty();
    }

    @Test
    void denies_when_every_application_is_rejected() {
        var validator = validator(assignedTo(APP_B, APP_A), gate(PrincipalMustAdministerAtLeastOneApplicationValidatorImplTests::denied));

        StepVerifier.create(validator.execute(ACTOR))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOfSatisfying(NotAuthorizedToAdministerTenantCatalogException.class, denial -> {
                            assertThat(denial.code()).isEqualTo("NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG");
                            assertThat(denial.evaluatedApplication()).contains(APP_A);
                        }))
                .verify();
        assertThat(consulted()).containsExactly(APP_A, APP_B);
    }

    @Test
    void a_gate_error_on_one_application_does_not_authorize_it_and_the_next_is_still_tried() {
        var validator = validator(assignedTo(APP_A, APP_B),
                gate(application -> application.equals(APP_A)
                        ? Mono.error(new IllegalStateException("opa caído")) : Mono.empty()));

        StepVerifier.create(validator.execute(ACTOR))
                .expectNext(APP_B)
                .verifyComplete();
        assertThat(consulted()).containsExactly(APP_A, APP_B);
    }

    @Test
    void denies_when_every_gate_call_errors() {
        var validator = validator(assignedTo(APP_A, APP_B),
                gate(application -> Mono.error(new IllegalStateException("opa caído"))));

        StepVerifier.create(validator.execute(ACTOR))
                .expectErrorSatisfies(error -> assertThat(error)
                        // nunca se propaga el error crudo del gate: es una denegación de negocio
                        .isInstanceOfSatisfying(NotAuthorizedToAdministerTenantCatalogException.class,
                                denial -> assertThat(denial.evaluatedApplication()).contains(APP_A)))
                .verify();
        assertThat(consulted()).containsExactly(APP_A, APP_B);
    }

    @Test
    void denies_when_resolving_the_applications_fails() {
        ResolveUserApplicationsUseCase failing = request -> Mono.error(new IllegalStateException("surreal caído"));
        var validator = validator(failing, gate(application -> Mono.empty()));

        StepVerifier.create(validator.execute(ACTOR))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOfSatisfying(NotAuthorizedToAdministerTenantCatalogException.class,
                                denial -> assertThat(denial.evaluatedApplication()).isEmpty()))
                .verify();
        assertThat(gateCalls).isEmpty();
    }

    @Test
    void does_nothing_until_subscribed() {
        var validator = validator(assignedTo(APP_A), gate(application -> Mono.empty()));

        Mono<ApplicationId> pending = validator.execute(ACTOR);

        assertThat(resolutions).isEmpty();
        assertThat(gateCalls).isEmpty();
        StepVerifier.create(pending).expectNext(APP_A).verifyComplete();
        assertThat(resolutions).hasSize(1);
        assertThat(gateCalls).hasSize(1);
    }

    @Test
    void a_previous_execution_does_not_leak_into_the_next_one() {
        // El validador es un singleton: lo recordado en una ejecución (MFA exigida, primera aplicación) no puede
        // sobrevivir a ella.
        AtomicInteger executions = new AtomicInteger();
        var validator = validator(assignedTo(APP_A, APP_B), gate(application -> switch (executions.get()) {
            case 0 -> Mono.error(new MfaEvidenceRequiredException(TENANT, application));
            case 1 -> denied(application);
            default -> application.equals(APP_B) ? Mono.empty() : denied(application);
        }));

        StepVerifier.create(validator.execute(ACTOR)).expectError(MfaEvidenceRequiredException.class).verify();
        executions.incrementAndGet();
        StepVerifier.create(validator.execute(ACTOR)).expectError(NotAuthorizedToAdministerTenantCatalogException.class).verify();
        executions.incrementAndGet();
        StepVerifier.create(validator.execute(ACTOR)).expectNext(APP_B).verifyComplete();
    }

    // ---- C6: MFA --------------------------------------------------------------------------------------------------

    @Test
    void propagates_the_first_mfa_requirement_when_no_application_permits() {
        MfaEvidenceRequiredException first = new MfaEvidenceRequiredException(TENANT, APP_A);
        MfaEvidenceRequiredException second = new MfaEvidenceRequiredException(TENANT, APP_B);
        var validator = validator(assignedTo(APP_B, APP_A), gate(application -> Mono.error(application.equals(APP_A) ? first : second)));

        StepVerifier.create(validator.execute(ACTOR))
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(first))
                .verify();
        assertThat(consulted()).containsExactly(APP_A, APP_B);
    }

    @Test
    void mfa_requirement_wins_over_plain_denials() {
        MfaEvidenceRequiredException mfa = new MfaEvidenceRequiredException(TENANT, APP_B);
        var validator = validator(assignedTo(APP_A, APP_B, APP_C), gate(application -> application.equals(APP_B)
                ? Mono.error(mfa) : denied(application)));

        StepVerifier.create(validator.execute(ACTOR))
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(mfa))
                .verify();
        assertThat(consulted()).containsExactly(APP_A, APP_B, APP_C);
    }

    @Test
    void a_later_permit_wins_over_an_earlier_mfa_requirement() {
        var validator = validator(assignedTo(APP_A, APP_B), gate(application -> application.equals(APP_A)
                ? Mono.error(new MfaEvidenceRequiredException(TENANT, application)) : Mono.empty()));

        StepVerifier.create(validator.execute(ACTOR))
                .expectNext(APP_B)
                .verifyComplete();
    }

    // ---- C9: aislamiento por inquilino ---------------------------------------------------------------------------

    @Test
    void resolves_applications_with_the_actors_user_and_tenant() {
        var validator = validator(assignedTo(APP_A), gate(application -> Mono.empty()));

        StepVerifier.create(validator.execute(ACTOR)).expectNext(APP_A).verifyComplete();

        assertThat(resolutions).containsExactly(new ResolveUserApplicationsRequest(USER, TENANT));
    }

    @Test
    void builds_the_application_request_with_tenant_user_subject_authentication_and_no_roles() {
        var validator = validator(assignedTo(APP_A), gate(application -> Mono.empty()));

        StepVerifier.create(validator.execute(ACTOR)).expectNext(APP_A).verifyComplete();

        assertThat(gateCalls).containsExactly(new AdministrationRequest(TENANT, APP_A, USER, SUBJECT, Set.of(), EVIDENCE));
    }
}
