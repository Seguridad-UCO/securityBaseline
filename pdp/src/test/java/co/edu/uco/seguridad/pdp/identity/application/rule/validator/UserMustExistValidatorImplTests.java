package co.edu.uco.seguridad.pdp.identity.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.impl.UserMustExistValidatorImpl;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.exception.UserNotFoundException;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.pdp.identity.domain.model.ExternalIdentity;
import co.edu.uco.seguridad.pdp.identity.domain.rule.UserMustExistRule;
import co.edu.uco.seguridad.pdp.identity.domain.rule.impl.UserMustExistRuleImpl;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

class UserMustExistValidatorImplTests {

    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final UserMustExistRule MUST_EXIST = new UserMustExistRuleImpl();
    private static final SecurityUser SECURITY_USER = SecurityUser.provision(USER, new TenantId("universidad-uco"),
            new Email("docente@uco.edu.co"), "Docente", Instant.parse("2026-09-12T00:00:00Z"));

    @Test
    void completes_when_the_user_exists() {
        UserMustExistValidatorImpl validator = new UserMustExistValidatorImpl(repositoryReturning(Mono.just(SECURITY_USER)), MUST_EXIST);

        StepVerifier.create(validator.execute(USER)).verifyComplete();
    }

    @Test
    void rejects_when_the_user_does_not_exist() {
        UserMustExistValidatorImpl validator = new UserMustExistValidatorImpl(repositoryReturning(Mono.empty()), MUST_EXIST);

        StepVerifier.create(validator.execute(USER))
                .expectError(UserNotFoundException.class)
                .verify();
    }

    private static SecurityUserRepository repositoryReturning(Mono<SecurityUser> result) {
        return new SecurityUserRepository() {
            @Override
            public Mono<ExternalIdentity> findIdentity(String issuer, String subject) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ExternalIdentity> findIdentityBySubject(String subject) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<SecurityUser> findByEmail(Email email) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<SecurityUser> findById(UserId userId) {
                return result;
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
