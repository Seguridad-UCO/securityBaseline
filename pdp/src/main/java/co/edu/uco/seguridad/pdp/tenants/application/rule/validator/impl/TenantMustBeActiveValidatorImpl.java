package co.edu.uco.seguridad.pdp.tenants.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.rule.validator.TenantMustBeActiveValidator;
import co.edu.uco.seguridad.pdp.tenants.application.secondaryport.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.TenantMustExistRule;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.TenantStatusMustBeActiveRule;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.model.TenantActivation;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.model.TenantExistence;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class TenantMustBeActiveValidatorImpl implements TenantMustBeActiveValidator {

    private final TenantRepository repository;
    private final TenantMustExistRule mustExist;
    private final TenantStatusMustBeActiveRule statusMustBeActive;

    public TenantMustBeActiveValidatorImpl(TenantRepository repository, TenantMustExistRule mustExist,
                                           TenantStatusMustBeActiveRule statusMustBeActive) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.TENANT_REPOSITORY);
        this.mustExist = Objects.requireNonNull(mustExist, RequiredArgumentMessages.TENANT_EXISTS_RULE);
        this.statusMustBeActive = Objects.requireNonNull(statusMustBeActive, RequiredArgumentMessages.ACTIVE_STATUS_RULE);
    }

    @Override
    public Mono<Void> execute(TenantId tenantId) {
        // La rama vacía nunca completa: TenantMustExistRule con registered=false siempre lanza.
        return repository.findStatusById(tenantId)
                .map(status -> new TenantActivation(tenantId, status))
                .switchIfEmpty(Mono.fromRunnable(() -> mustExist.execute(new TenantExistence(tenantId, false))))
                .doOnNext(statusMustBeActive::execute)
                .then();
    }
}
