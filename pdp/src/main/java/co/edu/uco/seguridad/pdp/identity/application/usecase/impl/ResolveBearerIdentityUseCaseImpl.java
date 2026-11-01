package co.edu.uco.seguridad.pdp.identity.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.ResolveBearerIdentityRequest;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ResolveBearerIdentityUseCase;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.pdp.identity.domain.model.ExternalIdentity;
import co.edu.uco.seguridad.pdp.identity.domain.rule.UserEmailMustBeAvailableRule;
import co.edu.uco.seguridad.pdp.identity.domain.rule.model.UserEmailAvailability;
import co.edu.uco.seguridad.pdp.tenants.application.rule.validator.TenantMustBeActiveValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Reconoce al sujeto Bearer por su identidad local; si nunca se vio, lo aprovisiona en el tenant del
 * claim (HU-038). Sin enlace por correo: un correo ajeno se rechaza.
 */
public final class ResolveBearerIdentityUseCaseImpl implements ResolveBearerIdentityUseCase {

    /** TLD reservado (RFC 2606): nunca colisiona con un correo real. */
    private static final String PLACEHOLDER_EMAIL_DOMAIN = "@bearer.invalid";

    private final SecurityUserRepository repository;
    private final TenantMustBeActiveValidator tenantMustBeActive;
    private final UserEmailMustBeAvailableRule emailMustBeAvailable;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public ResolveBearerIdentityUseCaseImpl(SecurityUserRepository repository,
                                            TenantMustBeActiveValidator tenantMustBeActive,
                                            UserEmailMustBeAvailableRule emailMustBeAvailable,
                                            IdentifierGenerator identifiers, TimeProvider time) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.USER_REPOSITORY);
        this.tenantMustBeActive = Objects.requireNonNull(tenantMustBeActive, RequiredArgumentMessages.TENANT_ACTIVE_VALIDATOR);
        this.emailMustBeAvailable = Objects.requireNonNull(emailMustBeAvailable, RequiredArgumentMessages.USER_EMAIL_AVAILABLE_RULE);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<UserId> execute(ResolveBearerIdentityRequest input) {
        return repository.findIdentityBySubject(input.subject())
                .map(ExternalIdentity::userId)
                .switchIfEmpty(Mono.defer(() -> provision(input)));
    }

    private Mono<UserId> provision(ResolveBearerIdentityRequest input) {
        return tenantMustBeActive.execute(input.tenantId())
                .then(Mono.defer(() -> {
                    Email email = effectiveEmail(input);
                    return repository.findByEmail(email)
                            .map(existing -> true)
                            .defaultIfEmpty(false)
                            .doOnNext(taken -> emailMustBeAvailable.execute(new UserEmailAvailability(email, taken)))
                            .flatMap(taken -> create(input, email));
                }));
    }

    private Mono<UserId> create(ResolveBearerIdentityRequest input, Email email) {
        SecurityUser user = SecurityUser.provision(new UserId(identifiers.next()), input.tenantId(), email,
                input.name(), time.now());
        ExternalIdentity identity = new ExternalIdentity(user.id(), input.issuer(), input.subject(), input.provider());
        return repository.createUserWithIdentityIfAbsent(user, identity).map(ExternalIdentity::userId);
    }

    /** Solo se confía en el correo del token si el IdP lo verificó; si no, uno reservado único por sujeto. */
    private static Email effectiveEmail(ResolveBearerIdentityRequest input) {
        return input.email().filter(email -> input.emailVerified())
                .orElseGet(() -> new Email(input.subject() + PLACEHOLDER_EMAIL_DOMAIN));
    }
}
