package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.AssignRoleRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignRoleUseCase;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.shared.cache.DistributedCachePort;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Construye un Assignment desde cero: por eso IdentifierGenerator y TimeProvider van en la firma
 * desde el primer día.
 *
 * <p>HU-023 (PLAN-HU-023.md §7): tras guardar, invoca
 * {@code cache.evict(assignment.userId(), assignment.applicationId())} — best-effort, el puerto
 * nunca propaga error (fail-open también en escritura, ver {@code DistributedCachePort}).</p>
 */
public final class AssignRoleUseCaseImpl implements AssignRoleUseCase {

    private final AssignRoleRulesValidator rules;
    private final AssignmentRepository repository;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;
    private final DistributedCachePort cache;

    public AssignRoleUseCaseImpl(AssignRoleRulesValidator rules, AssignmentRepository repository,
                                 IdentifierGenerator identifiers, TimeProvider time, DistributedCachePort cache) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.ASSIGN_ROLE_RULES_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ASSIGNMENT_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
        this.cache = Objects.requireNonNull(cache, RequiredArgumentMessages.DISTRIBUTED_CACHE_PORT);
    }

    @Override
    public Mono<AssignmentResponse> execute(AssignRoleRequest input) {
        return rules.execute(input)
                .then(Mono.defer(() -> repository.save(Assignment.assign(new AssignmentId(identifiers.next()),
                        input.userId(), input.tenantId(), input.applicationId(), input.roleId(), time.now()))))
                .flatMap(assignment -> cache.evict(assignment.userId(), assignment.applicationId()).thenReturn(assignment))
                .map(assignment -> new AssignmentResponse(assignment.id(), assignment.userId(), assignment.tenantId(),
                        assignment.applicationId(), assignment.roleId(), assignment.validity().validFrom(),
                        assignment.validity().validUntil()));
    }
}
