package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationOwnershipQuery;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveActiveRolesRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.AccessAuditRepository;
import co.edu.uco.seguridad.pdp.authorization.domain.event.AccessEvent;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.resources.domain.exception.ProtectedResourceNotFoundException;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El caso de uso no decide: traduce lo que ya decidieron los validadores ajenos. Cada caso
 * comprueba que, cuando un colaborador no deberia alcanzarse, no se alcanza (poison pill).
 */
class AuthorizeUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ResourcePath PATH = new ResourcePath("/estudiantes");
    private static final AccessRequest REQUEST =
            new AccessRequest(TENANT, "test-subject", APPLICATION, PATH, HttpVerb.GET, "req-1", "corr-1",
                    Optional.empty(), Set.of());
    private static final UUID USER_ID_VALUE = UUID.randomUUID();
    private static final UserId USER_ID = new UserId(USER_ID_VALUE);
    private static final AccessRequest REQUEST_WITH_USER =
            new AccessRequest(TENANT, "test-subject", APPLICATION, PATH, HttpVerb.GET, "req-1", "corr-1",
                    Optional.of(USER_ID), Set.of());
    private static final UUID DECISION_ID = UUID.randomUUID();
    private static final Instant DECIDED_AT = Instant.parse("2026-09-06T00:00:00Z");

    private static final co.edu.uco.seguridad.pdp.authorization.application.rule.validator.ActiveRoleNamesLookupValidator NEVER_ROLES_LOOKUP =
            request -> {
                throw new AssertionError("must not reach the roles lookup");
            };

    // Fake comun para los casos que no ejercitan la auditoria directamente: recordAudit(...) es el
    // ultimo paso de la cadena y se alcanza sin importar que camino produjo la decision (HU-007,
    // criterio 1: toda decision emite evento), asi que este fake acepta el guardado en silencio en
    // vez de tratarlo como un colaborador que no deberia alcanzarse.
    private static final AccessAuditRepository ACCEPTING_AUDIT = new AccessAuditRepository() {
        @Override
        public Mono<Void> save(AccessEvent event) {
            return Mono.empty();
        }

        @Override
        public Flux<AccessEvent> findByCorrelationId(String correlationId) {
            throw new AssertionError("must not reach the audit query");
        }
    };

    @Test
    void reports_tenant_mismatch_when_the_application_does_not_exist() {
        AuthorizeUseCaseImpl useCase = new AuthorizeUseCaseImpl(
                query -> Mono.error(new ApplicationNotFoundException(query.applicationId())),
                lookup -> {
                    throw new AssertionError("must not reach the resource lookup");
                },
                NEVER_ROLES_LOOKUP, ACCEPTING_AUDIT,
                request -> {
                    throw new AssertionError("must not reach the policy port");
                },
                () -> DECISION_ID, () -> DECIDED_AT);

        StepVerifier.create(useCase.execute(REQUEST))
                .assertNext(decision -> {
                    assertThat(decision.state()).isEqualTo(DecisionState.DENY);
                    assertThat(decision.reasonCode()).isEqualTo(ReasonCode.TENANT_MISMATCH);
                    assertThat(decision.correlationId()).isEqualTo("corr-1");
                    assertThat(decision.decisionId()).isEqualTo(DECISION_ID);
                    assertThat(decision.decidedAt()).isEqualTo(DECIDED_AT);
                    assertThat(decision.policyReferences()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void queries_application_ownership_with_the_requesters_own_tenant_and_still_denies_when_it_belongs_to_another() {
        // El validador real colapsa "no existe" y "existe pero es de otro inquilino" en la misma
        // ApplicationNotFoundException (existsByTenantAndId es una sola comprobacion compuesta), asi
        // que el caso de uso no puede -ni debe- distinguirlos. Lo que si puede fallar es que consulte
        // con el tenant equivocado; eso es lo que este caso protege, capturando la query recibida.
        List<ApplicationOwnershipQuery> received = new ArrayList<>();
        AuthorizeUseCaseImpl useCase = new AuthorizeUseCaseImpl(
                query -> {
                    received.add(query);
                    return Mono.error(new ApplicationNotFoundException(query.applicationId()));
                },
                lookup -> {
                    throw new AssertionError("must not reach the resource lookup");
                },
                NEVER_ROLES_LOOKUP, ACCEPTING_AUDIT,
                request -> {
                    throw new AssertionError("must not reach the policy port");
                },
                () -> DECISION_ID, () -> DECIDED_AT);

        StepVerifier.create(useCase.execute(REQUEST))
                .assertNext(decision -> {
                    assertThat(decision.state()).isEqualTo(DecisionState.DENY);
                    assertThat(decision.reasonCode()).isEqualTo(ReasonCode.TENANT_MISMATCH);
                })
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().tenantId()).isEqualTo(REQUEST.tenantId());
        assertThat(received.getFirst().applicationId()).isEqualTo(REQUEST.applicationId());
    }

    @Test
    void reports_no_applicable_policy_when_the_resource_does_not_exist() {
        AuthorizeUseCaseImpl useCase = new AuthorizeUseCaseImpl(
                query -> Mono.empty(),
                lookup -> Mono.error(new ProtectedResourceNotFoundException(lookup.applicationId(), lookup.path(), lookup.method())),
                NEVER_ROLES_LOOKUP, ACCEPTING_AUDIT,
                request -> {
                    throw new AssertionError("must not reach the policy port");
                },
                () -> DECISION_ID, () -> DECIDED_AT);

        StepVerifier.create(useCase.execute(REQUEST))
                .assertNext(decision -> {
                    assertThat(decision.state()).isEqualTo(DecisionState.DENY);
                    assertThat(decision.reasonCode()).isEqualTo(ReasonCode.NO_APPLICABLE_POLICY);
                    assertThat(decision.decisionId()).isEqualTo(DECISION_ID);
                    assertThat(decision.decidedAt()).isEqualTo(DECIDED_AT);
                })
                .verifyComplete();
    }

    @Test
    void delegates_to_the_policy_port_when_application_and_resource_exist() {
        AccessDecision expected = new AccessDecision(UUID.randomUUID(), DecisionState.DENY,
                ReasonCode.NO_APPLICABLE_POLICY, List.of(), "req-1", "corr-1", Instant.parse("2026-09-06T00:00:00Z"));
        AuthorizeUseCaseImpl useCase = new AuthorizeUseCaseImpl(
                query -> Mono.empty(), lookup -> Mono.empty(), NEVER_ROLES_LOOKUP, ACCEPTING_AUDIT,
                request -> Mono.just(expected),
                // recordAudit(...) si genera un eventId y un instante propios para el evento de
                // auditoria, aunque la decision ya venga resuelta del puerto de politicas.
                () -> DECISION_ID, () -> DECIDED_AT);

        StepVerifier.create(useCase.execute(REQUEST))
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    void delegates_to_the_policy_port_with_resolved_role_names_when_subject_user_id_is_present() {
        AccessDecision expected = new AccessDecision(UUID.randomUUID(), DecisionState.DENY,
                ReasonCode.NO_APPLICABLE_POLICY, List.of(), "req-1", "corr-1", Instant.parse("2026-09-06T00:00:00Z"));
        List<AccessRequest> received = new ArrayList<>();
        AuthorizeUseCaseImpl useCase = new AuthorizeUseCaseImpl(
                query -> Mono.empty(), lookup -> Mono.empty(),
                request -> {
                    assertThat(request).isEqualTo(new ResolveActiveRolesRequest(USER_ID, APPLICATION));
                    return Mono.just(Set.of("Coordinador académico"));
                },
                ACCEPTING_AUDIT,
                request -> {
                    received.add(request);
                    return Mono.just(expected);
                },
                // recordAudit(...) si genera un eventId y un instante propios para el evento de
                // auditoria, aunque la decision ya venga resuelta del puerto de politicas.
                () -> DECISION_ID, () -> DECIDED_AT);

        StepVerifier.create(useCase.execute(REQUEST_WITH_USER))
                .expectNext(expected)
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().subjectRoles()).containsExactly("Coordinador académico");
    }

    @Test
    void reports_indeterminate_when_the_policy_port_fails() {
        AuthorizeUseCaseImpl useCase = new AuthorizeUseCaseImpl(
                query -> Mono.empty(), lookup -> Mono.empty(), NEVER_ROLES_LOOKUP, ACCEPTING_AUDIT,
                request -> Mono.error(new RuntimeException("OPA unreachable")),
                () -> DECISION_ID, () -> DECIDED_AT);

        StepVerifier.create(useCase.execute(REQUEST))
                .assertNext(decision -> {
                    assertThat(decision.state()).isEqualTo(DecisionState.INDETERMINATE);
                    assertThat(decision.reasonCode()).isEqualTo(ReasonCode.CONTEXT_UNAVAILABLE);
                    assertThat(decision.decisionId()).isEqualTo(DECISION_ID);
                })
                .verifyComplete();
    }

    @Test
    void reports_indeterminate_when_the_application_lookup_fails_for_a_technical_reason() {
        AuthorizeUseCaseImpl useCase = new AuthorizeUseCaseImpl(
                query -> Mono.error(new RuntimeException("SurrealDB unreachable")),
                lookup -> {
                    throw new AssertionError("must not reach the resource lookup");
                },
                NEVER_ROLES_LOOKUP, ACCEPTING_AUDIT,
                request -> {
                    throw new AssertionError("must not reach the policy port");
                },
                () -> DECISION_ID, () -> DECIDED_AT);

        StepVerifier.create(useCase.execute(REQUEST))
                .assertNext(decision -> {
                    assertThat(decision.state()).isEqualTo(DecisionState.INDETERMINATE);
                    assertThat(decision.reasonCode()).isEqualTo(ReasonCode.CONTEXT_UNAVAILABLE);
                    assertThat(decision.decisionId()).isEqualTo(DECISION_ID);
                })
                .verifyComplete();
    }

    @Test
    void records_an_audit_event_matching_the_returned_decision() {
        AccessDecision expected = new AccessDecision(DECISION_ID, DecisionState.DENY, ReasonCode.NO_APPLICABLE_POLICY,
                List.of(), "req-1", "corr-1", DECIDED_AT);
        List<AccessEvent> saved = new ArrayList<>();
        AccessAuditRepository capturingAudit = new AccessAuditRepository() {
            @Override
            public Mono<Void> save(AccessEvent event) {
                saved.add(event);
                return Mono.empty();
            }

            @Override
            public Flux<AccessEvent> findByCorrelationId(String correlationId) {
                throw new AssertionError("must not reach the audit query");
            }
        };
        AuthorizeUseCaseImpl useCase = new AuthorizeUseCaseImpl(query -> Mono.empty(), lookup -> Mono.empty(),
                NEVER_ROLES_LOOKUP, capturingAudit, request -> Mono.just(expected),
                // recordAudit(...) si genera un eventId y un instante propios para el evento de
                // auditoria, aunque la decision ya venga resuelta del puerto de politicas.
                () -> DECISION_ID, () -> DECIDED_AT);

        StepVerifier.create(useCase.execute(REQUEST))
                .expectNext(expected)
                .verifyComplete();

        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().decisionId()).isEqualTo(expected.decisionId());
        assertThat(saved.getFirst().correlationId()).isEqualTo(expected.correlationId());
        assertThat(saved.getFirst().state()).isEqualTo(expected.state());
        assertThat(saved.getFirst().reasonCode()).isEqualTo(expected.reasonCode());
    }

    @Test
    void an_audit_failure_does_not_alter_the_returned_decision() {
        AccessDecision expected = new AccessDecision(DECISION_ID, DecisionState.DENY, ReasonCode.NO_APPLICABLE_POLICY,
                List.of(), "req-1", "corr-1", DECIDED_AT);
        AccessAuditRepository failingAudit = new AccessAuditRepository() {
            @Override
            public Mono<Void> save(AccessEvent event) {
                return Mono.error(new RuntimeException("SurrealDB unreachable"));
            }

            @Override
            public Flux<AccessEvent> findByCorrelationId(String correlationId) {
                throw new AssertionError("must not reach the audit query");
            }
        };
        AuthorizeUseCaseImpl useCase = new AuthorizeUseCaseImpl(query -> Mono.empty(), lookup -> Mono.empty(),
                NEVER_ROLES_LOOKUP, failingAudit, request -> Mono.just(expected),
                // recordAudit(...) si genera un eventId y un instante propios para el evento de
                // auditoria, aunque la decision ya venga resuelta del puerto de politicas.
                () -> DECISION_ID, () -> DECIDED_AT);

        StepVerifier.create(useCase.execute(REQUEST))
                .expectNext(expected)
                .verifyComplete();
    }
}
