package co.edu.uco.seguridad.pdp.identity.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.UserMustExistValidator;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.domain.rule.UserMustExistRule;
import co.edu.uco.seguridad.pdp.identity.domain.rule.model.UserExistence;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class UserMustExistValidatorImpl implements UserMustExistValidator {

    private final SecurityUserRepository repository;
    private final UserMustExistRule mustExist;

    public UserMustExistValidatorImpl(SecurityUserRepository repository, UserMustExistRule mustExist) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.USER_REPOSITORY);
        this.mustExist = Objects.requireNonNull(mustExist, RequiredArgumentMessages.USER_RULE);
    }

    @Override
    public Mono<Void> execute(UserId input) {
        return repository.findById(input)
                .switchIfEmpty(Mono.fromRunnable(() -> mustExist.execute(new UserExistence(input, false))))
                .then();
    }
}
