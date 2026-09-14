package co.edu.uco.seguridad.pdp.identity.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.domain.model.ExternalIdentity;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class SubjectUserIdLookupValidatorImpl implements SubjectUserIdLookupValidator {

    private final SecurityUserRepository repository;

    public SubjectUserIdLookupValidatorImpl(SecurityUserRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.USER_REPOSITORY);
    }

    @Override
    public Mono<UserId> execute(String subject) {
        return repository.findIdentityBySubject(subject).map(ExternalIdentity::userId);
    }
}
