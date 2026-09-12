package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.dto.request.raw.RegisterIntegrationRawRequest;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.dto.response.IntegrationWebResponse;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.interactor.RegisterIntegrationInteractor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;

/** Controlador del plano de control: solo adapta HTTP y delega al interactor. */
@RestController
@RequestMapping("/internal/v1/integrations")
public final class IntegrationRegistrationController {

    private final RegisterIntegrationInteractor interactor;

    public IntegrationRegistrationController(RegisterIntegrationInteractor interactor) {
        this.interactor = interactor;
    }

    @PutMapping("/{applicationId}/{environment}")
    public Mono<IntegrationWebResponse> register(@PathVariable String applicationId, @PathVariable String environment,
                                                  @RequestHeader(name = "Authorization", required = false) String authorization,
                                                  @RequestBody RegistrationBody request) {
        return interactor.execute(new RegisterIntegrationRawRequest(applicationId, environment, authorization,
                request.backendUrl(), request.audience()));
    }

    public record RegistrationBody(URI backendUrl, String audience) {
    }
}
