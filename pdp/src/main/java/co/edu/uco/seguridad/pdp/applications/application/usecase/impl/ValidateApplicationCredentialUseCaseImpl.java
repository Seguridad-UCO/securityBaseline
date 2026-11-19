package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ValidateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.application.usecase.ValidateApplicationCredentialUseCase;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.exception.AmbiguousApplicationNameException;
import co.edu.uco.seguridad.pdp.applications.domain.exception.InvalidApplicationCredentialException;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash;
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
        return repository.findUniqueByName(input.applicationName())
                .flatMap(application -> Mono.fromCallable(() -> {
                    rule.execute(new ApplicationCredentialValidity(application.id(),
                            application.enabledEnvironments().contains(input.environment())
                                    && hasher.matches(input.secret(), application.credentialHash().value())));
                    return application;
                }))
                .flatMap(application -> upgradeLegacyHash(application, input.secret()).thenReturn(application.tenantId()))
                .onErrorMap(AmbiguousApplicationNameException.class, error -> new InvalidApplicationCredentialException())
                .switchIfEmpty(Mono.error(new InvalidApplicationCredentialException()));
    }

    /**
     * SU-02: una credencial correcta guardada en un formato anterior (BCrypt) se reescribe con el actual,
     * así esa aplicación paga el hash lento una sola vez. Best-effort: si guardar falla, la validación ya
     * resuelta no cambia y se reintentará en la siguiente.
     */
    private Mono<Void> upgradeLegacyHash(Application application, String secret) {
        if (!hasher.needsRehash(application.credentialHash().value())) {
            return Mono.empty();
        }
        return Mono.defer(() -> repository.updateCredentialHash(application.id(),
                        new ApplicationCredentialHash(hasher.hash(secret))))
                .onErrorResume(error -> Mono.empty());
    }
}
