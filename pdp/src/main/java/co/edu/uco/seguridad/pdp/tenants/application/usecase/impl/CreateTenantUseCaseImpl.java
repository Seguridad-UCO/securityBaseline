package co.edu.uco.seguridad.pdp.tenants.application.usecase.impl;

import co.edu.uco.seguridad.pdp.tenants.application.primaryport.request.CreateTenantRequest;
import co.edu.uco.seguridad.pdp.tenants.application.primaryport.response.TenantResponse;
import co.edu.uco.seguridad.pdp.tenants.application.secondaryport.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.CreateTenantUseCase;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.model.TenantCodeAvailability;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.TenantCodeMustBeUniqueRule;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * El caso de uso resuelve el dato y la regla decide sobre él: una sola regla, sin validador de por
 * medio, porque no hay nada que coordinar ni otro módulo que la consuma.
 */
public final class CreateTenantUseCaseImpl implements CreateTenantUseCase {

    private final TenantCodeMustBeUniqueRule mustBeUnique;
    private final TenantRepository repository;

    public CreateTenantUseCaseImpl(TenantCodeMustBeUniqueRule mustBeUnique, TenantRepository repository) {
        this.mustBeUnique = Objects.requireNonNull(mustBeUnique, RequiredArgumentMessages.UNIQUE_RULE);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.TENANT_REPOSITORY);
    }

    @Override
    public Mono<TenantResponse> execute(CreateTenantRequest dto) {
        return repository.existsById(dto.id())
                .doOnNext(registered -> mustBeUnique.execute(new TenantCodeAvailability(dto.id(), registered)))
                .then(Mono.fromSupplier(() -> Tenant.register(dto.id(), dto.name())))
                .flatMap(repository::save)
                .map(tenant -> new TenantResponse(tenant.id(), tenant.name(), tenant.status()));
    }
}
