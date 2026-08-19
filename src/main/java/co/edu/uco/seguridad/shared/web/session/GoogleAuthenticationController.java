package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.pdp.platform.application.PlatformAdministrationService;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/** Intercambia una evidencia Google firmada por una sesión local HttpOnly; el token nunca se guarda. */
@RestController
@RequestMapping("/api/v1/auth")
final class GoogleAuthenticationController {
    record GoogleCredentialRequest(String credential) { }
    private final ReactiveJwtDecoder googleDecoder;
    private final PlatformAdministrationService users;
    private final String clientId;
    private final WebSessionServerSecurityContextRepository sessions = new WebSessionServerSecurityContextRepository();
    GoogleAuthenticationController(@Qualifier("googleJwtDecoder") ReactiveJwtDecoder googleDecoder, PlatformAdministrationService users,
                                   @Value("${pdp.security.google.client-id:}") String clientId) {
        this.googleDecoder = googleDecoder; this.users = users; this.clientId = clientId;
    }
    @PostMapping("/google")
    Mono<Void> google(@RequestBody GoogleCredentialRequest request, ServerWebExchange exchange) {
        if (request.credential() == null || request.credential().isBlank() || clientId.isBlank())
            return Mono.error(new IllegalArgumentException("Google no está configurado en el backend."));
        return googleDecoder.decode(request.credential())
                .flatMap(jwt -> validate(jwt).then(users.provision(jwt.getIssuer().toString(), jwt.getSubject(),
                        jwt.getClaimAsString("email"), jwt.getClaimAsString("name"))))
                .flatMap(user -> saveSession(exchange, user));
    }
    private Mono<Jwt> validate(Jwt jwt) {
        List<String> audiences = jwt.getAudience();
        boolean issuer = "https://accounts.google.com".equals(jwt.getIssuer().toString()) || "accounts.google.com".equals(jwt.getIssuer().toString());
        boolean verified = Boolean.TRUE.equals(jwt.getClaim("email_verified"));
        if (!issuer || !audiences.contains(clientId) || !verified || jwt.getClaimAsString("email") == null)
            return Mono.error(new IllegalArgumentException("La credencial de Google no es válida para esta aplicación."));
        return Mono.just(jwt);
    }
    private Mono<Void> saveSession(ServerWebExchange exchange, LocalUserPrincipal user) {
        var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
        var context = new SecurityContextImpl(authentication);
        exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
        return sessions.save(exchange, context);
    }
}
