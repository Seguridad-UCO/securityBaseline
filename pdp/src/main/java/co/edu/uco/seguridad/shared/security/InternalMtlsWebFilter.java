package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Falla cerrado en {@code /internal/v1/**} si la petición no trae certificado de cliente, o si su
 * sujeto no está en {@link InternalMtlsProperties#allowedSubjects()} (HU-003, decisión D2). El
 * certificado se lee de {@code exchange.getRequest().getSslInfo()} — nunca de un header: en
 * despliegue directo (no App Service, ver T3) es Netty quien valida la cadena en el handshake, así
 * que un certificado no confiable ni siquiera llega aquí. Ausencia de certificado nunca es permiso:
 * es un rechazo explícito y testeable.
 *
 * <p>Se registra en la cadena de seguridad de {@code /internal/v1/**} (no en
 * {@code SecurityConfiguration} ni en {@code KeycloakSecurityConfiguration}, que siguen siendo el
 * canal BFF) — la implementación de la Configuration que lo cablea es trabajo del implementador,
 * no de este esqueleto.
 */
public final class InternalMtlsWebFilter implements WebFilter {

    private final InternalMtlsProperties properties;

    public InternalMtlsWebFilter(InternalMtlsProperties properties) {
        this.properties = Objects.requireNonNull(properties, RequiredArgumentMessages.INTERNAL_MTLS_PROPERTIES);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        throw new UnsupportedOperationException("pendiente: HU-003");
    }
}
