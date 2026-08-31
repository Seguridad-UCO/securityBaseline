package co.edu.uco.seguridad.pdp.tenants.application.rule.impl;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.exception.DuplicateTenantException;
import co.edu.uco.seguridad.pdp.tenants.application.secondaryport.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantCodeMustBeUniqueRule;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class TenantCodeMustBeUniqueRuleImpl implements TenantCodeMustBeUniqueRule {

    private final TenantRepository repository;

    public TenantCodeMustBeUniqueRuleImpl(TenantRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.TENANT_REPOSITORY);
    }

    @Override
    public Mono<Void> execute(TenantId tenantId) {
        return repository.existsById(tenantId)
                .filter(Boolean::booleanValue)
                .flatMap(exists -> Mono.<Void>error(new DuplicateTenantException(tenantId)));
    }
}
