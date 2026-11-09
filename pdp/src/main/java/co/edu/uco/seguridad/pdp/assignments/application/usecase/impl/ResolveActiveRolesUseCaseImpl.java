package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveActiveRolesRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ActiveRolesResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveActiveRolesUseCase;
import co.edu.uco.seguridad.shared.cache.DistributedCachePort;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * HU-023 (PLAN-HU-023.md §7): {@code execute} intenta {@code cache.get(...)} primero; en el miss
 * (que ya cubre tanto "no está en caché" como "Redis no respondió", por el fail-open del puerto),
 * consulta {@code AssignmentRepository} como antes y puebla la caché con {@code cache.put(...)}
 * antes de devolver la respuesta.
 */
public final class ResolveActiveRolesUseCaseImpl implements ResolveActiveRolesUseCase {

    private final AssignmentRepository repository;
    private final TimeProvider time;
    private final DistributedCachePort cache;

    public ResolveActiveRolesUseCaseImpl(AssignmentRepository repository, TimeProvider time, DistributedCachePort cache) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ASSIGNMENT_REPOSITORY);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
        this.cache = Objects.requireNonNull(cache, RequiredArgumentMessages.DISTRIBUTED_CACHE_PORT);
    }

    @Override
    public Mono<ActiveRolesResponse> execute(ResolveActiveRolesRequest input) {
        return cache.get(input.userId(), input.applicationId())
                .switchIfEmpty(Mono.defer(() -> repository.findActiveRoleIdsFor(input.userId(), input.applicationId(), time.now())
                        .flatMap(roleIds -> cache.put(input.userId(), input.applicationId(), roleIds).thenReturn(roleIds))))
                .map(roleIds -> new ActiveRolesResponse(input.userId(), input.applicationId(), roleIds));
    }
}
