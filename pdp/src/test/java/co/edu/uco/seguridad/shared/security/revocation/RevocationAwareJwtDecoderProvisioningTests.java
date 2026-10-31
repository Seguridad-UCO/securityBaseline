package co.edu.uco.seguridad.shared.security.revocation;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.ResolveBearerIdentityRequest;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ResolveBearerIdentityUseCase;
import co.edu.uco.seguridad.pdp.tenants.domain.exception.TenantNotActiveException;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantStatus;
import co.edu.uco.seguridad.shared.security.BearerIdentityClaim;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-038: ruta {@code provisioning} del decoder Bearer (aprovisiona antes de consultar revocación). */
class RevocationAwareJwtDecoderProvisioningTests {

    private static final UserId RESOLVED = new UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final UserId FORGED = new UserId(UUID.fromString("99999999-9999-9999-9999-999999999999"));
    private static final Instant IAT = Instant.parse("2026-10-30T10:00:00Z");

    @Test
    void provisions_the_identity_before_checking_revocation_and_delivers_a_stamped_jwt() {
        List<String> events = new ArrayList<>();
        List<ResolveBearerIdentityRequest> requests = new ArrayList<>();
        ResolveBearerIdentityUseCase identities = request -> {
            events.add("identities");
            requests.add(request);
            return Mono.just(RESOLVED);
        };
        var decoder = RevocationAwareJwtDecoder.provisioning(delegateReturning(jwt("universidad-uco", null)),
                new FakeRevocation(events, false, false, false), identities);

        StepVerifier.create(decoder.decode("t"))
                .assertNext(jwt -> assertThat(jwt.getClaimAsString(BearerIdentityClaim.NAME))
                        .isEqualTo(RESOLVED.value().toString()))
                .verifyComplete();

        assertThat(events.get(0)).isEqualTo("identities");
        assertThat(events).contains("isRevoked", "isTokenRevoked");
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).subject()).isEqualTo("sub-1");
        assertThat(requests.get(0).tenantId().value()).isEqualTo("universidad-uco");
    }

    @Test
    void revocation_is_checked_against_the_resolved_user_id() {
        List<UserId> asked = new ArrayList<>();
        var revocation = new FakeRevocation(new ArrayList<>(), false, false, false) {
            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                asked.add(subject);
                return Mono.just(false);
            }
        };
        var decoder = RevocationAwareJwtDecoder.provisioning(delegateReturning(jwt("universidad-uco", null)),
                revocation, request -> Mono.just(RESOLVED));

        StepVerifier.create(decoder.decode("t")).expectNextCount(1).verifyComplete();

        assertThat(asked).containsExactly(RESOLVED);
    }

    @Test
    void refuses_a_token_without_tenant_and_touches_neither_identities_nor_revocation() {
        List<String> events = new ArrayList<>();
        var decoder = RevocationAwareJwtDecoder.provisioning(delegateReturning(jwt(null, null)),
                new FakeRevocation(events, false, false, false), request -> {
                    events.add("identities");
                    return Mono.just(RESOLVED);
                });

        StepVerifier.create(decoder.decode("t"))
                .expectError(JwtValidationException.class)
                .verify();

        assertThat(events).isEmpty();
    }

    @Test
    void a_suspended_tenant_becomes_a_jwt_validation_error() {
        List<String> events = new ArrayList<>();
        var decoder = RevocationAwareJwtDecoder.provisioning(delegateReturning(jwt("universidad-uco", null)),
                new FakeRevocation(events, false, false, false),
                request -> Mono.error(new TenantNotActiveException(request.tenantId(), TenantStatus.SUSPENDED)));

        StepVerifier.create(decoder.decode("t"))
                .expectError(JwtValidationException.class)
                .verify();

        assertThat(events).isEmpty();
    }

    @Test
    void any_provisioning_failure_is_closed_as_a_jwt_validation_error() {
        var decoder = RevocationAwareJwtDecoder.provisioning(delegateReturning(jwt("universidad-uco", null)),
                new FakeRevocation(new ArrayList<>(), false, false, false),
                request -> Mono.error(new IllegalStateException("surreal caido")));

        StepVerifier.create(decoder.decode("t"))
                .expectError(JwtValidationException.class)
                .verify();
    }

    @Test
    void overwrites_an_incoming_internal_claim_with_the_resolved_user_id() {
        var decoder = RevocationAwareJwtDecoder.provisioning(
                delegateReturning(jwt("universidad-uco", FORGED.value().toString())),
                new FakeRevocation(new ArrayList<>(), false, false, false), request -> Mono.just(RESOLVED));

        StepVerifier.create(decoder.decode("t"))
                .assertNext(jwt -> assertThat(jwt.getClaimAsString(BearerIdentityClaim.NAME))
                        .isEqualTo(RESOLVED.value().toString()))
                .verifyComplete();
    }

    @Test
    void a_token_issued_before_revokeAllSince_stays_rejected_for_a_freshly_provisioned_user() {
        var decoder = RevocationAwareJwtDecoder.provisioning(delegateReturning(jwt("universidad-uco", null)),
                new FakeRevocation(new ArrayList<>(), true, false, false), request -> Mono.just(RESOLVED));

        StepVerifier.create(decoder.decode("t")).expectError(JwtValidationException.class).verify();
    }

    @Test
    void a_revoked_jti_stays_rejected_for_a_freshly_provisioned_user() {
        var decoder = RevocationAwareJwtDecoder.provisioning(delegateReturning(jwt("universidad-uco", null)),
                new FakeRevocation(new ArrayList<>(), false, true, false), request -> Mono.just(RESOLVED));

        StepVerifier.create(decoder.decode("t")).expectError(JwtValidationException.class).verify();
    }

    @Test
    void a_revoked_client_stays_rejected_for_a_freshly_provisioned_user() {
        var decoder = RevocationAwareJwtDecoder.provisioning(delegateReturning(jwt("universidad-uco", null)),
                new FakeRevocation(new ArrayList<>(), false, false, true), request -> Mono.just(RESOLVED));

        StepVerifier.create(decoder.decode("t")).expectError(JwtValidationException.class).verify();
    }

    @Test
    void a_revocation_lookup_failure_is_closed_as_a_jwt_validation_error() {
        var revocation = new FakeRevocation(new ArrayList<>(), false, false, false) {
            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                return Mono.error(new IllegalStateException("redis caido"));
            }
        };
        var decoder = RevocationAwareJwtDecoder.provisioning(delegateReturning(jwt("universidad-uco", null)),
                revocation, request -> Mono.just(RESOLVED));

        StepVerifier.create(decoder.decode("t")).expectError(JwtValidationException.class).verify();
    }

    @Test
    void a_jvm_error_is_not_swallowed_into_a_401() {
        var decoder = RevocationAwareJwtDecoder.provisioning(delegateReturning(jwt("universidad-uco", null)),
                new FakeRevocation(new ArrayList<>(), false, false, false),
                request -> Mono.error(new AssertionError("fallo de la JVM")));

        StepVerifier.create(decoder.decode("t"))
                .expectError(AssertionError.class)
                .verify();
    }

    @Test
    void a_delegate_failure_propagates_without_provisioning() {
        List<String> events = new ArrayList<>();
        ReactiveJwtDecoder failing = token -> Mono.error(new JwtException("firma invalida"));
        var decoder = RevocationAwareJwtDecoder.provisioning(failing,
                new FakeRevocation(events, false, false, false), request -> {
                    events.add("identities");
                    return Mono.just(RESOLVED);
                });

        StepVerifier.create(decoder.decode("t"))
                .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(JwtException.class)
                        .hasMessage("firma invalida"))
                .verify();

        assertThat(events).isEmpty();
    }

    private static ReactiveJwtDecoder delegateReturning(Jwt jwt) {
        return token -> Mono.just(jwt);
    }

    private static Jwt jwt(String tenant, String internalClaim) {
        Jwt.Builder builder = Jwt.withTokenValue("token-value")
                .header("alg", "RS256")
                .issuer("https://idp.test/realms/uco")
                .subject("sub-1")
                .claim("jti", "jti-1")
                .claim("azp", "web-client")
                .issuedAt(IAT)
                .expiresAt(IAT.plusSeconds(300));
        if (tenant != null) builder.claim("tenant", tenant);
        if (internalClaim != null) builder.claim(BearerIdentityClaim.NAME, internalClaim);
        return builder.build();
    }

    private static class FakeRevocation implements TokenRevocationPort {

        private final List<String> events;
        private final boolean revokedSince;
        private final boolean tokenRevoked;
        private final boolean clientRevoked;

        FakeRevocation(List<String> events, boolean revokedSince, boolean tokenRevoked, boolean clientRevoked) {
            this.events = events;
            this.revokedSince = revokedSince;
            this.tokenRevoked = tokenRevoked;
            this.clientRevoked = clientRevoked;
        }

        @Override
        public Mono<Void> revokeAllSince(UserId subject, Instant since) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
            events.add("isRevoked");
            return Mono.just(revokedSince);
        }

        @Override
        public Mono<Boolean> isTokenRevoked(UserId subject, String tokenId) {
            events.add("isTokenRevoked");
            return Mono.just(tokenRevoked);
        }

        @Override
        public Mono<Void> revokeClientSince(UserId subject, String clientId, Instant since) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Mono<Boolean> isClientRevoked(UserId subject, String clientId, Instant issuedAt) {
            events.add("isClientRevoked");
            return Mono.just(clientRevoked);
        }
    }
}
