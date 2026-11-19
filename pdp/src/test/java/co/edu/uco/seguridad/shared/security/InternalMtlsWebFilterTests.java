package co.edu.uco.seguridad.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D2: sin certificado de cliente, o con un sujeto fuera de la lista admitida, la cadena nunca se
 * invoca y la respuesta es 403 — falla cerrado, sin lanzar. El certificado del handshake ya pasó
 * por la validación de cadena de Netty antes de llegar aquí (javadoc de {@link InternalMtlsWebFilter}),
 * así que estas pruebas no construyen un certificado criptográficamente válido: solo uno cuyo
 * Subject DN el filtro pueda leer — un fake, no un mock (sb-testing), del mismo modo que un fake de
 * repositorio implementa solo lo que el test usa y lanza {@code UnsupportedOperationException} en
 * el resto. La validación de la cadena de confianza real la cubre la prueba de integración con
 * certificados efímeros (plan §9, {@code InternalSecurityChainIntegrationTests}), no esta clase.
 */
class InternalMtlsWebFilterTests {

    private static final InternalMtlsProperties PROPERTIES =
            new InternalMtlsProperties(true, "ca.pem", List.of("CN=pep"));

    private static final WebFilterChain NEVER_CALLED = exchange -> {
        throw new AssertionError("must not call the chain: mTLS must reject first");
    };

    @Test
    void rejects_a_request_without_a_client_certificate() {
        InternalMtlsWebFilter filter = new InternalMtlsWebFilter(PROPERTIES);
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.post("/internal/v1/access-decisions").build());

        StepVerifier.create(filter.filter(exchange, NEVER_CALLED)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void rejects_a_client_certificate_whose_subject_is_not_admitted() {
        InternalMtlsWebFilter filter = new InternalMtlsWebFilter(PROPERTIES);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/internal/v1/access-decisions")
                        .sslInfo(sslInfoWithSubject("CN=intruso"))
                        .build());

        StepVerifier.create(filter.filter(exchange, NEVER_CALLED)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void continues_the_chain_when_the_certificate_subject_is_admitted() {
        InternalMtlsWebFilter filter = new InternalMtlsWebFilter(PROPERTIES);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/internal/v1/access-decisions")
                        .sslInfo(sslInfoWithSubject("CN=pep"))
                        .build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        WebFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chainCalled).isTrue();
    }

    @Test
    void explicitly_disabled_local_mtls_does_not_require_a_client_certificate() {
        InternalMtlsWebFilter filter = new InternalMtlsWebFilter(
                new InternalMtlsProperties(false, "", List.of()));
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.post("/internal/v1/access-decisions").build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, ex -> {
            chainCalled.set(true);
            return Mono.empty();
        })).verifyComplete();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private static SslInfo sslInfoWithSubject(String distinguishedName) {
        X509Certificate certificate = certificateWithSubject(distinguishedName);
        return new SslInfo() {
            @Override
            public String getSessionId() {
                return "fake-session";
            }

            @Override
            public X509Certificate[] getPeerCertificates() {
                return new X509Certificate[]{certificate};
            }
        };
    }

    /**
     * Solo implementa lo que {@link InternalMtlsWebFilter} necesita leer (el Subject DN, por
     * cualquiera de las dos formas estándar de exponerlo). El resto lanza — nadie más lo llama: la
     * validez criptográfica ya la garantizó el handshake, no este fake.
     */
    private static X509Certificate certificateWithSubject(String distinguishedName) {
        X500Principal subject = new X500Principal(distinguishedName);
        return new X509Certificate() {
            @Override
            public X500Principal getSubjectX500Principal() {
                return subject;
            }

            @Override
            public Principal getSubjectDN() {
                return subject;
            }

            @Override
            public void checkValidity() {
            }

            @Override
            public void checkValidity(Date date) {
            }

            @Override
            public int getVersion() {
                throw new UnsupportedOperationException();
            }

            @Override
            public BigInteger getSerialNumber() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Principal getIssuerDN() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Date getNotBefore() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Date getNotAfter() {
                throw new UnsupportedOperationException();
            }

            @Override
            public byte[] getTBSCertificate() {
                throw new UnsupportedOperationException();
            }

            @Override
            public byte[] getSignature() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getSigAlgName() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getSigAlgOID() {
                throw new UnsupportedOperationException();
            }

            @Override
            public byte[] getSigAlgParams() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean[] getIssuerUniqueID() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean[] getSubjectUniqueID() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean[] getKeyUsage() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getBasicConstraints() {
                throw new UnsupportedOperationException();
            }

            @Override
            public byte[] getEncoded() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void verify(PublicKey key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void verify(PublicKey key, String sigProvider) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PublicKey getPublicKey() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toString() {
                return "fake-certificate:" + distinguishedName;
            }

            @Override
            public Set<String> getCriticalExtensionOIDs() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<String> getNonCriticalExtensionOIDs() {
                throw new UnsupportedOperationException();
            }

            @Override
            public byte[] getExtensionValue(String oid) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean hasUnsupportedCriticalExtension() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
