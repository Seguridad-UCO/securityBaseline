package co.edu.uco.seguridad.pdp.identity.application.rule.impl;

import co.edu.uco.seguridad.pdp.identity.application.exception.UserNotFoundException;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.application.rule.UserMustExistRule;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class UserMustExistRuleImpl implements UserMustExistRule {

    private final SecurityUserRepository repository;

    public UserMustExistRuleImpl(SecurityUserRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.USER_REPOSITORY);
    }

    @Override
    public Mono<SecurityUser> execute(UserId userId) {
        return repository.findById(userId)
                .switchIfEmpty(Mono.error(() -> new UserNotFoundException(userId)));
    }
}
