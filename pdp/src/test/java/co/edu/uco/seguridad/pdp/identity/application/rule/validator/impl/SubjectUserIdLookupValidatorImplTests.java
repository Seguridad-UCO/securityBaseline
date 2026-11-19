package co.edu.uco.seguridad.pdp.identity.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.pdp.identity.domain.model.ExternalIdentity;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

/**
 * Traduce, no decide: si {@code identity} no tiene ninguna identidad externa con ese subject, el
 * validador queda vacío — no inventa un rechazo (PLAN-HU-015.md §14).
 */
class SubjectUserIdLookupValidatorImplTests {

    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final String SUBJECT = "keycloak-subject-123";

    @Test
    void resolves_the_user_id_linked_to_an_existing_external_identity() {
        SubjectUserIdLookupValidatorImpl validator = new SubjectUserIdLookupValidatorImpl(
                repositoryResolving(Mono.just(new ExternalIdentity(USER, "https://issuer.example", SUBJECT, "keycloak"))));

        StepVerifier.create(validator.execute(SUBJECT)).expectNext(USER).verifyComplete();
    }

    @Test
    void resolves_empty_when_no_external_identity_has_that_subject() {
        SubjectUserIdLookupValidatorImpl validator = new SubjectUserIdLookupValidatorImpl(repositoryResolving(Mono.empty()));

        StepVerifier.create(validator.execute(SUBJECT)).verifyComplete();
    }

    private static SecurityUserRepository repositoryResolving(Mono<ExternalIdentity> result) {
        return new SecurityUserRepository() {
            @Override
            public Mono<ExternalIdentity> findIdentity(String issuer, String subject) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ExternalIdentity> findIdentityBySubject(String subject) {
                return result;
            }

            @Override
            public Mono<SecurityUser> findByEmail(Email email) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<SecurityUser> findById(UserId userId) {
                throw new UnsupportedOperationException();
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
