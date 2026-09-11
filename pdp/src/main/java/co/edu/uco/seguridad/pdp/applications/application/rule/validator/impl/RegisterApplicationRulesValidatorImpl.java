package co.edu.uco.seguridad.pdp.applications.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.RegisterApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationNameAvailability;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationNameMustBeUniqueForTenantRule;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationNameMustNotBeReservedRule;
import co.edu.uco.seguridad.pdp.tenants.application.rule.validator.TenantMustBeActiveValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * El validador es quien hace la E/S: resuelve contra el puerto lo que cada regla necesita saber y
 * después deja decidir a las reglas, que son puras. Ejecuta primero lo más barato —la única regla
 * que no necesita consultar nada rechaza DTOs malos antes de tocar ningún repositorio.
 */
public final class RegisterApplicationRulesValidatorImpl implements RegisterApplicationRulesValidator {

    private final ApplicationNameMustNotBeReservedRule nameMustNotBeReserved;
    private final TenantMustBeActiveValidator tenantMustBeActive;
    private final ApplicationNameMustBeUniqueForTenantRule nameMustBeUnique;
    private final ApplicationRepository repository;

    public RegisterApplicationRulesValidatorImpl(ApplicationNameMustNotBeReservedRule nameMustNotBeReserved,
                                                 TenantMustBeActiveValidator tenantMustBeActive,
                                                 ApplicationNameMustBeUniqueForTenantRule nameMustBeUnique,
                                                 ApplicationRepository repository) {
        this.nameMustNotBeReserved = Objects.requireNonNull(nameMustNotBeReserved, RequiredArgumentMessages.RESERVED_NAME_RULE);
        this.tenantMustBeActive = Objects.requireNonNull(tenantMustBeActive, RequiredArgumentMessages.TENANT_ACTIVE_VALIDATOR);
        this.nameMustBeUnique = Objects.requireNonNull(nameMustBeUnique, RequiredArgumentMessages.UNIQUE_RULE);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.APPLICATION_REPOSITORY);
    }

    @Override
    public Mono<Void> execute(RegisterApplicationRequest dto) {
        return Mono.fromRunnable(() -> nameMustNotBeReserved.execute(dto.name()))
                .then(tenantMustBeActive.execute(dto.tenantId()))
                .then(repository.existsByTenantAndName(dto.tenantId(), dto.name()))
                .doOnNext(registered -> nameMustBeUnique.execute(
                        new ApplicationNameAvailability(dto.tenantId(), dto.name(), registered)))
                .then();
    }
}
