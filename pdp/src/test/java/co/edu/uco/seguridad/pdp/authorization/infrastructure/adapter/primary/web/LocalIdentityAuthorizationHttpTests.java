package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.PdpApplication;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.OpaFixtureServer;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-038, perfil por defecto (HMAC): el decoder {@code LocalIdentityStampingJwtDecoder} sella el
 * {@code UserId} de un sujeto ya vinculado ({@code linkTestIdentity}), de modo que
 * {@code authorize} por Bearer llega a ALLOW tras asignar el rol. Sin aprovisionar.
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LocalIdentityAuthorizationHttpTests extends AbstractSurrealDbIntegrationTest {

    private static final String UCO = BearerHttpFixture.UCO;

    private static OpaFixtureServer opa;

    @LocalServerPort
    int port;

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

    @Test
    void a_linked_subject_reaches_allow_through_the_default_hmac_decoder() {
        String prefix = "hu038l-" + UUID.randomUUID().toString().substring(0, 8);
        String adminSubject = prefix + "-admin";
        String subject = prefix + "-usuario";
        linkTestIdentity(new TenantId(UCO), adminSubject);
        UserId user = linkTestIdentity(new TenantId(UCO), subject);
        BearerHttpFixture fixture = new BearerHttpFixture(client(), adminSubject);
        String applicationId = fixture.registerApplication(prefix + "-app");
        String resourceId = fixture.registerResourceAndFindId(applicationId, "/estudiantes", "GET");
        String roleName = prefix + "-rol";
        String roleId = fixture.defineTenantRole(roleName);
        fixture.grantResource(roleId, resourceId);
        opa.respondWithForPath(BearerHttpFixture.AUTHORIZATION_DECISION_PATH, 200, BearerHttpFixture.DENY_BODY);
        String token = TestJwtSupport.signedToken(UCO, subject);

        fixture.authorize(token, applicationId, "/estudiantes", "GET")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.state").isEqualTo("DENY");

        fixture.assignRole(roleId, user, applicationId);
        opa.respondWithForPath(BearerHttpFixture.AUTHORIZATION_DECISION_PATH, 200, BearerHttpFixture.ALLOW_BODY);

        fixture.authorize(token, applicationId, "/estudiantes", "GET")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.state").isEqualTo("ALLOW");
        assertThat(opa.lastRequestBody()).contains(roleName);
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }
}
