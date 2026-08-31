package co.edu.uco.seguridad.pdp.tenants.application.usecase.impl;

import co.edu.uco.seguridad.pdp.tenants.application.primaryport.request.CreateTenantRequest;
import co.edu.uco.seguridad.pdp.tenants.application.primaryport.response.TenantResponse;
import co.edu.uco.seguridad.pdp.tenants.application.secondaryport.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantCodeMustBeUniqueRule;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.CreateTenantUseCase;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class CreateTenantUseCaseImpl implements CreateTenantUseCase {

    private final TenantCodeMustBeUniqueRule mustBeUnique;
    private final TenantRepository repository;

    public CreateTenantUseCaseImpl(TenantCodeMustBeUniqueRule mustBeUnique, TenantRepository repository) {
        this.mustBeUnique = Objects.requireNonNull(mustBeUnique, RequiredArgumentMessages.TENANT_RULE);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.TENANT_REPOSITORY);
    }

    @Override
    public Mono<TenantResponse> execute(CreateTenantRequest dto) {
        return mustBeUnique.execute(dto.id())
                .then(Mono.fromSupplier(() -> Tenant.register(dto.id(), dto.name())))
                .flatMap(repository::save)
                .map(tenant -> new TenantResponse(tenant.id(), tenant.name(), tenant.status()));
    }
}
