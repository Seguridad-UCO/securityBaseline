package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.RevokeAssignmentRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeAssignmentUseCase;
import co.edu.uco.seguridad.shared.cache.DistributedCachePort;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Transforma lo que el validador ya encontró (Assignment.revoke) y lo guarda. No decide nada.
 *
 * <p>Una asignación no es una credencial. Revocarla no invalida el JWT ni la sesión del usuario:
 * el PDP vuelve a resolver los hechos de autorización para cada decisión. De ese modo, la
 * denegación es inmediata y una reasignación posterior funciona con la misma sesión. La revocación
 * de tokens queda reservada para eventos de identidad, como compromiso de cuenta o cierre global
 * de sesión.</p>
 *
 * <p>Tras revocar, invoca
 * {@code cache.evict(assignment.userId(), assignment.applicationId())} — best-effort, el puerto
 * nunca propaga error (fail-open también en escritura, ver {@code DistributedCachePort}).</p>
 */
public final class RevokeAssignmentUseCaseImpl implements RevokeAssignmentUseCase {

    private final RevokeAssignmentRulesValidator rules;
    private final AssignmentRepository repository;
    private final TimeProvider time;
    private final DistributedCachePort cache;

    public RevokeAssignmentUseCaseImpl(RevokeAssignmentRulesValidator rules, AssignmentRepository repository, TimeProvider time,
                                       DistributedCachePort cache) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.REVOKE_ASSIGNMENT_RULES_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ASSIGNMENT_REPOSITORY);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
        this.cache = Objects.requireNonNull(cache, RequiredArgumentMessages.DISTRIBUTED_CACHE_PORT);
    }

    @Override
    public Mono<Void> execute(RevokeAssignmentRequest input) {
        return rules.execute(input)
                .map(assignment -> assignment.revoke(time.now()))
                .flatMap(repository::save)
                .flatMap(assignment -> cache.evict(assignment.userId(), assignment.applicationId()));
    }
}
