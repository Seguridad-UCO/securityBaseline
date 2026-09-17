package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.RevokeAssignmentRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeAssignmentUseCase;
import co.edu.uco.seguridad.shared.cache.DistributedCachePort;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Transforma lo que el validador ya encontró (Assignment.revoke) y lo guarda. No decide nada.
 *
 * <p>HU-022 (PLAN-HU-022.md §7): tras guardar, invoca
 * {@code revocation.revokeAllSince(assignment.userId(), time.now())} — fail-closed también en
 * escritura: si la revocación falla, la operación completa falla, porque un "removido pero no
 * revocado" es exactamente el hueco de seguridad que esta historia cierra (a diferencia del puerto
 * de caché de HU-023, que sí es fail-open).</p>
 *
 * <p>HU-023 (PLAN-HU-023.md §7): tras revocar, invoca además
 * {@code cache.evict(assignment.userId(), assignment.applicationId())} — best-effort, el puerto
 * nunca propaga error (fail-open también en escritura, ver {@code DistributedCachePort}).</p>
 */
public final class RevokeAssignmentUseCaseImpl implements RevokeAssignmentUseCase {

    private final RevokeAssignmentRulesValidator rules;
    private final AssignmentRepository repository;
    private final TimeProvider time;
    private final TokenRevocationPort revocation;
    private final DistributedCachePort cache;

    public RevokeAssignmentUseCaseImpl(RevokeAssignmentRulesValidator rules, AssignmentRepository repository, TimeProvider time,
            TokenRevocationPort revocation, DistributedCachePort cache) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.REVOKE_ASSIGNMENT_RULES_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ASSIGNMENT_REPOSITORY);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
        this.revocation = Objects.requireNonNull(revocation, RequiredArgumentMessages.TOKEN_REVOCATION_PORT);
        this.cache = Objects.requireNonNull(cache, RequiredArgumentMessages.DISTRIBUTED_CACHE_PORT);
    }

    @Override
    public Mono<Void> execute(RevokeAssignmentRequest input) {
        return rules.execute(input)
                .map(assignment -> assignment.revoke(time.now()))
                .flatMap(repository::save)
                .flatMap(assignment -> revocation.revokeAllSince(assignment.userId(), time.now())
                        .then(Mono.defer(() -> cache.evict(assignment.userId(), assignment.applicationId()))));
    }
}
