package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.ValidateApplicationCredentialRawRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationCredentialValidationWebResponse;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.ValidateApplicationCredentialInteractor;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador primario HTTP del canal interno de validación de credenciales (HU-013). Protegido por
 * la misma cadena mTLS + evidencia JWT de {@code /internal/v1/**} (HU-003) sin tocar
 * {@code InternalSecurityConfiguration} — su {@code securityMatcher} ya es un comodín.
 *
 * <p>La respuesta **no** se envuelve en {@code ApiResponse} (mismo criterio D6 de HU-003): es un
 * canal máquina-a-máquina.</p>
 */
@RestController
@RequestMapping("/internal/v1/applications")
final class InternalApplicationCredentialController {

    private final ValidateApplicationCredentialInteractor interactor;

    InternalApplicationCredentialController(ValidateApplicationCredentialInteractor interactor) {
        this.interactor = Objects.requireNonNull(interactor,
                RequiredArgumentMessages.VALIDATE_APPLICATION_CREDENTIAL_INTERACTOR);
    }

    @PostMapping("/names/{applicationName}/credential-validations")
    Mono<ResponseEntity<ApplicationCredentialValidationWebResponse>> validate(@PathVariable String applicationName,
            @RequestBody ValidateApplicationCredentialRawRequest body) {
        return interactor.execute(new ValidateApplicationCredentialRawRequest(applicationName, body.secret()))
                .map(ResponseEntity::ok);
    }
}
