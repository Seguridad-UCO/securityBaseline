package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.pdp.identity.domain.model.ExternalIdentity;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.schema.SurrealIdentitySchemaInitializer;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-038: creación atómica de usuario + vínculo contra una SurrealDB real. Requiere Docker. Cada
 * prueba usa un prefijo único porque la base es compartida y no se limpia.
 */
class SurrealSecurityUserRepositoryProvisioningTests extends AbstractSurrealDbIntegrationTest {

    private static final String ISSUER = "https://idp.test/realms/uco";
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");

    private static SecurityUserRepository repository;

    @BeforeAll
    static void setUp() {
        SurrealDbProperties properties = new SurrealDbProperties(
                "http://%s:%d".formatted(SURREALDB.getHost(), SURREALDB.getMappedPort(8000)),
                "pdp", "pdp", "root", "root");
        WebClient webClient = WebClient.builder()
                .baseUrl(properties.url())
                .defaultHeaders(headers -> {
                    headers.setBasicAuth(properties.username(), properties.password());
                    headers.set("surreal-ns", properties.namespace());
                    headers.set("surreal-db", properties.database());
                })
                .build();
        SurrealDbClient client = new SurrealDbClient(webClient, new ObjectMapper(), properties);
        new SurrealIdentitySchemaInitializer(client).run(null);
        repository = new SurrealSecurityUserRepository(client);
    }

    @Test
    void creates_the_user_and_the_link_together() {
        String unique = unique();
        SecurityUser user = user(unique);
        ExternalIdentity identity = identity(user, unique);

        StepVerifier.create(repository.createUserWithIdentityIfAbsent(user, identity))
                .expectNext(identity)
                .verifyComplete();

        StepVerifier.create(repository.findIdentity(ISSUER, "sub-" + unique))
                .assertNext(found -> assertThat(found.userId()).isEqualTo(user.id()))
                .verifyComplete();
        StepVerifier.create(repository.findById(user.id()))
                .assertNext(found -> {
                    assertThat(found.tenantId()).isEqualTo(TENANT);
                    assertThat(found.email()).isEqualTo(user.email());
                })
                .verifyComplete();
    }

    @Test
    void two_concurrent_creations_for_the_same_subject_leave_one_user_one_link_and_one_winner() {
        String unique = unique();
        SecurityUser first = user(unique);
        SecurityUser second = new SecurityUser(new UserId(UUID.randomUUID()), TENANT,
                new Email("otro-" + unique + "@uco.edu.co"), "Otro", NOW, NOW);

        List<ExternalIdentity> results = Flux.merge(
                        repository.createUserWithIdentityIfAbsent(first, identity(first, unique)),
                        repository.createUserWithIdentityIfAbsent(second, identity(second, unique)))
                .collectList().block();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).userId()).isEqualTo(results.get(1).userId());
        UserId winner = results.get(0).userId();
        assertThat(winner).isIn(first.id(), second.id());
        UserId loser = winner.equals(first.id()) ? second.id() : first.id();
        StepVerifier.create(repository.findById(loser)).verifyComplete();
        StepVerifier.create(repository.findIdentity(ISSUER, "sub-" + unique))
                .assertNext(found -> assertThat(found.userId()).isEqualTo(winner))
                .verifyComplete();
    }

    @Test
    void an_email_collision_leaves_neither_an_orphan_user_nor_a_link() {
        String unique = unique();
        SecurityUser existing = user(unique);
        repository.createUserWithIdentityIfAbsent(existing, identity(existing, unique)).block();

        SecurityUser newcomer = new SecurityUser(new UserId(UUID.randomUUID()), TENANT, existing.email(),
                "Otro", NOW, NOW);
        ExternalIdentity newcomerLink = new ExternalIdentity(newcomer.id(), ISSUER, "otro-sub-" + unique, "keycloak-local");

        StepVerifier.create(repository.createUserWithIdentityIfAbsent(newcomer, newcomerLink))
                .expectError()
                .verify();

        StepVerifier.create(repository.findById(newcomer.id())).verifyComplete();
        StepVerifier.create(repository.findIdentity(ISSUER, "otro-sub-" + unique)).verifyComplete();
    }

    @Test
    void a_link_collision_leaves_no_user_without_link() {
        String unique = unique();
        SecurityUser existing = user(unique);
        ExternalIdentity existingLink = identity(existing, unique);
        repository.createUserWithIdentityIfAbsent(existing, existingLink).block();

        SecurityUser newcomer = new SecurityUser(new UserId(UUID.randomUUID()), TENANT,
                new Email("nuevo-" + unique + "@uco.edu.co"), "Nuevo", NOW, NOW);

        Mono<ExternalIdentity> attempt = repository.createUserWithIdentityIfAbsent(newcomer,
                new ExternalIdentity(newcomer.id(), ISSUER, "sub-" + unique, "keycloak-local"));

        StepVerifier.create(attempt)
                .assertNext(winner -> assertThat(winner.userId()).isEqualTo(existing.id()))
                .verifyComplete();
        StepVerifier.create(repository.findById(newcomer.id())).verifyComplete();
    }

    private static String unique() {
        return "hu038-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static SecurityUser user(String unique) {
        return new SecurityUser(new UserId(UUID.randomUUID()), TENANT, new Email(unique + "@uco.edu.co"),
                "Usuario " + unique, NOW, NOW);
    }

    private static ExternalIdentity identity(SecurityUser user, String unique) {
        return new ExternalIdentity(user.id(), ISSUER, "sub-" + unique, "keycloak-local");
    }
}
