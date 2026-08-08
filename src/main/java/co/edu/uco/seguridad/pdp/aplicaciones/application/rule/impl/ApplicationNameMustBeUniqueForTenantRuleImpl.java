package co.edu.uco.seguridad.pdp.aplicaciones.application.rule.impl;

import co.edu.uco.seguridad.pdp.aplicaciones.application.exception.DuplicateApplicationException;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.secondary.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.aplicaciones.application.rule.ApplicationNameMustBeUniqueForTenantRule;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ApplicationNameMustBeUniqueForTenantRuleImpl implements ApplicationNameMustBeUniqueForTenantRule {

    private final ApplicationRepository repository;

    public ApplicationNameMustBeUniqueForTenantRuleImpl(ApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "se requiere repositorio de aplicación");
    }

    @Override
    public Mono<Void> execute(RegisterApplicationRequest dto) {
        return repository.existsByTenantAndName(dto.tenantId(), dto.name())
                .filter(Boolean::booleanValue)
                .flatMap(exists -> Mono.<Void>error(
                        new DuplicateApplicationException(dto.tenantId(), dto.name())));
    }
}
