package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.PdpApplication;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.OpaFixtureServer;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ResolveBearerIdentityUseCase;
import co.edu.uco.seguridad.pdp.identity.domain.model.ExternalIdentity;
import co.edu.uco.seguridad.shared.security.BearerIdentityClaim;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import co.edu.uco.seguridad.shared.security.revocation.RevocationAwareJwtDecoder;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-038: el canal Bearer reconoce al usuario. Sustituye {@code jwtDecoder} por
 * {@code RevocationAwareJwtDecoder.provisioning(...)} con el {@code ResolveBearerIdentityUseCase} real
 * y una revocación en memoria (nada revocado), sobre SurrealDB real. OPA es el
 * {@link OpaFixtureServer}: no evalúa políticas, así que el ALLOW se obtiene cambiando su respuesta
 * después de asignar el rol, y lo que se afirma sobre la identidad es el {@code input} que el PDP le
 * envió (roles resueltos del sujeto).
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(BearerAuthorizationHttpTests.ProvisioningDecoderConfiguration.class)
class BearerAuthorizationHttpTests extends AbstractSurrealDbIntegrationTest {

    private static final String UCO = BearerHttpFixture.UCO;
    private static final String ADMIN_SUBJECT = "bearer-admin-subject";

    private static OpaFixtureServer opa;

    @LocalServerPort
    int port;

    @Autowired
    SecurityUserRepository users;

    private BearerHttpFixture fixture;
    private String prefix;
    private String applicationId;
    private String roleName;
    private String roleId;

    @BeforeAll
    static void startOpaFixture() throws Exception {
        opa = OpaFixtureServer.start();
        opa.respondWithForPath(BearerHttpFixture.ADMINISTRATION_DECISION_PATH, 200, BearerHttpFixture.ALLOW_BODY);
    }

    @AfterAll
    static void stopOpaFixture() {
        opa.stop();
    }

    @DynamicPropertySource
    static void opaConnectionProperties(DynamicPropertyRegistry registry) {
        registry.add("pdp.opa.base-url", () -> opa.baseUrl());
    }

    @DynamicPropertySource
    static void mfaEvidenceProperties(DynamicPropertyRegistry registry) {
        registry.add("pdp.security.mfa.claim", () -> "acr");
        registry.add("pdp.security.mfa.accepted-values", () -> TestJwtSupport.MFA_ACCEPTED_ACR);
    }

    @BeforeEach
    void seedApplicationRoleAndGrant() {
        prefix = "hu038-" + UUID.randomUUID().toString().substring(0, 8);
        opa.respondWithForPath(BearerHttpFixture.AUTHORIZATION_DECISION_PATH, 200, BearerHttpFixture.DENY_BODY);
        linkTestIdentity(new TenantId(UCO), ADMIN_SUBJECT);
        fixture = new BearerHttpFixture(client(), ADMIN_SUBJECT);
        applicationId = fixture.registerApplication(prefix + "-app");
        String resourceId = fixture.registerResourceAndFindId(applicationId, "/estudiantes", "GET");
        roleName = prefix + "-rol";
        roleId = fixture.defineTenantRole(roleName);
        fixture.grantResource(roleId, resourceId);
    }

    @Test
    void first_use_of_an_unknown_subject_provisions_user_and_identity_in_the_tenant_of_the_claim() {
        String subject = prefix + "-nuevo";

        client().get().uri("/api/v1/applications/" + applicationId + "/resources")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(UCO, subject))
                .exchange()
                .expectStatus().isOk();

        StepVerifier.create(users.findIdentityBySubject(subject))
                .assertNext(identity -> assertThat(identity.subject()).isEqualTo(subject))
                .verifyComplete();
        StepVerifier.create(users.findIdentityBySubject(subject).map(ExternalIdentity::userId).flatMap(users::findById))
                .assertNext(user -> assertThat(user.tenantId().value()).isEqualTo(UCO))
                .verifyComplete();
    }

    @Test
    void authorize_by_bearer_denies_before_the_role_and_allows_after_assigning_it() {
        String subject = prefix + "-usuario";
        String token = TestJwtSupport.signedToken(UCO, subject);

        fixture.authorize(token, applicationId, "/estudiantes", "GET")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.state").isEqualTo("DENY");
        assertThat(opa.lastRequestBody()).doesNotContain(roleName);

        UserId provisioned = users.findIdentityBySubject(subject).map(ExternalIdentity::userId).block();
        assertThat(provisioned).isNotNull();
        fixture.assignRole(roleId, provisioned, applicationId);
        opa.respondWithForPath(BearerHttpFixture.AUTHORIZATION_DECISION_PATH, 200, BearerHttpFixture.ALLOW_BODY);

        fixture.authorize(token, applicationId, "/estudiantes", "GET")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.state").isEqualTo("ALLOW");
        assertThat(opa.lastRequestBody()).contains(roleName);
    }

    @Test
    void an_unknown_tenant_gets_401_and_nothing_is_created() {
        String subject = prefix + "-sin-tenant";

        client().get().uri("/api/v1/applications/" + applicationId + "/resources")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken("tenant-que-no-existe-038", subject))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.code").isEqualTo("UNAUTHORIZED");

        StepVerifier.create(users.findIdentityBySubject(subject)).verifyComplete();
    }

    @Test
    void a_forged_internal_claim_never_impersonates_a_user_with_permissions() {
        String privilegedSubject = prefix + "-privilegiado";
        UserId privileged = linkTestIdentity(new TenantId(UCO), privilegedSubject);
        fixture.assignRole(roleId, privileged, applicationId);
        opa.respondWithForPath(BearerHttpFixture.AUTHORIZATION_DECISION_PATH, 200, BearerHttpFixture.DENY_BODY);

        String attackerToken = signedTokenWithInternalClaim(UCO, prefix + "-atacante", privileged);

        fixture.authorize(attackerToken, applicationId, "/estudiantes", "GET")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.state").value(state -> assertThat(state).isNotEqualTo("ALLOW"));
        assertThat(opa.lastRequestBody()).doesNotContain(roleName).doesNotContain(privileged.value().toString());
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private static String signedTokenWithInternalClaim(String tenant, String subject, UserId forged) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject).issuer(TestJwtSupport.ISSUER).audience(TestJwtSupport.AUDIENCE)
                    .jwtID(UUID.randomUUID().toString()).claim("tenant", tenant)
                    .claim(BearerIdentityClaim.NAME, forged.value().toString())
                    .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(300))).build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(TestJwtSupport.SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception cause) {
            throw new IllegalStateException("no se pudo firmar el token de prueba", cause);
        }
    }

    @TestConfiguration
    static class ProvisioningDecoderConfiguration {

        /** La cadena de seguridad inyecta el bean {@code jwtDecoder} por nombre: se envuelve ese, no se añade otro. */
        @Bean
        static BeanPostProcessor provisioningJwtDecoder(ObjectProvider<ResolveBearerIdentityUseCase> identities) {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if ("jwtDecoder".equals(beanName) && bean instanceof ReactiveJwtDecoder hmacDecoder) {
                        return RevocationAwareJwtDecoder.provisioning(hmacDecoder, new NothingRevoked(),
                                request -> identities.getObject().execute(request));
                    }
                    return bean;
                }
            };
        }
    }

    private static final class NothingRevoked implements TokenRevocationPort {

        @Override
        public Mono<Void> revokeAllSince(UserId subject, Instant since) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
            return Mono.empty();
        }

        @Override
        public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
            return Mono.just(false);
        }

        @Override
        public Mono<Boolean> isTokenRevoked(UserId subject, String tokenId) {
            return Mono.just(false);
        }

        @Override
        public Mono<Void> revokeClientSince(UserId subject, String clientId, Instant since) {
            return Mono.empty();
        }

        @Override
        public Mono<Boolean> isClientRevoked(UserId subject, String clientId, Instant issuedAt) {
            return Mono.just(false);
        }
    }
}
