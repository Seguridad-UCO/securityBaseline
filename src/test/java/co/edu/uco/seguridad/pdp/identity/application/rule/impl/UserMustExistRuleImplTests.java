package co.edu.uco.seguridad.pdp.identity.application.rule.impl;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.identity.application.exception.UserNotFoundException;
import co.edu.uco.seguridad.pdp.identity.application.port.secondary.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.domain.Email;
import co.edu.uco.seguridad.pdp.identity.domain.ExternalIdentity;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.UserId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

class UserMustExistRuleImplTests {

    private static final UserId ID = new UserId(UUID.randomUUID());

    @Test
    void hands_back_the_user_when_it_exists() {
        SecurityUser user = user();
        UserMustExistRuleImpl rule = new UserMustExistRuleImpl(repositoryReturning(user));

        StepVerifier.create(rule.execute(ID)).expectNext(user).verifyComplete();
    }

    @Test
    void refuses_a_user_that_does_not_exist() {
        UserMustExistRuleImpl rule = new UserMustExistRuleImpl(repositoryReturning(null));

        StepVerifier.create(rule.execute(ID)).expectError(UserNotFoundException.class).verify();
    }

    private static SecurityUser user() {
        return SecurityUser.provision(ID, new TenantId("universidad-uco"), new Email("david@uco.edu.co"), "David",
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static SecurityUserRepository repositoryReturning(SecurityUser user) {
        return new SecurityUserRepository() {
            @Override
            public Mono<ExternalIdentity> findIdentity(String issuer, String subject) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<SecurityUser> findByEmail(Email email) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<SecurityUser> findById(UserId userId) {
                return user == null ? Mono.empty() : Mono.just(user);
            }

            @Override
            public Mono<String> providerFor(UserId userId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<SecurityUser> save(SecurityUser user) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> linkIdentity(ExternalIdentity identity) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Flux<SecurityUser> findAll() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
