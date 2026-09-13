package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ValidateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.application.usecase.ValidateApplicationCredentialUseCase;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationCredentialMustBeValidRule;
import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationCredentialValidity;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.CredentialHasher;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link ValidateApplicationCredentialUseCase} (HU-013). Ausencia de la
 * aplicación y secreto incorrecto colapsan al mismo booleano ({@code valid = false}) antes de
 * llegar a la regla, para que la excepción resultante nunca distinga la causa.
 */
public final class ValidateApplicationCredentialUseCaseImpl implements ValidateApplicationCredentialUseCase {

    private final ApplicationCredentialMustBeValidRule rule;
    private final ApplicationRepository repository;
    private final CredentialHasher hasher;

    public ValidateApplicationCredentialUseCaseImpl(ApplicationCredentialMustBeValidRule rule,
            ApplicationRepository repository, CredentialHasher hasher) {
        this.rule = Objects.requireNonNull(rule, RequiredArgumentMessages.APPLICATION_CREDENTIAL_MUST_BE_VALID_RULE);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.APPLICATION_REPOSITORY);
        this.hasher = Objects.requireNonNull(hasher, RequiredArgumentMessages.CREDENTIAL_HASHER);
    }

    @Override
    public Mono<TenantId> execute(ValidateApplicationCredentialRequest input) {
        return repository.findCredentialHashById(input.applicationId())
                .map(credentialHash -> hasher.matches(input.secret(), credentialHash.value()))
                .defaultIfEmpty(false)
                .doOnNext(valid -> rule.execute(new ApplicationCredentialValidity(input.applicationId(), valid)))
                .then(Mono.defer(() -> repository.findTenantIdById(input.applicationId())));
    }
}
