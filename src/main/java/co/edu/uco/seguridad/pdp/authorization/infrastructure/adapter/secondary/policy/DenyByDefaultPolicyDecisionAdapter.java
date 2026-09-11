package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.PolicyDecisionPort;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * Deniega por defecto (ADR-012): sin politica publicada, la respuesta correcta es DENY con
 * NO_APPLICABLE_POLICY, no un error. No es un mock temporal — HU-004 lo sustituye por el cliente
 * real de OPA sin tocar {@link PolicyDecisionPort}.
 */
public final class DenyByDefaultPolicyDecisionAdapter implements PolicyDecisionPort {

    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public DenyByDefaultPolicyDecisionAdapter(IdentifierGenerator identifiers, TimeProvider time) {
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<AccessDecision> execute(AccessRequest input) {
        return Mono.fromSupplier(() -> new AccessDecision(identifiers.next(), DecisionState.DENY,
                ReasonCode.NO_APPLICABLE_POLICY, List.of(), input.correlationId(), time.now()));
    }
}
