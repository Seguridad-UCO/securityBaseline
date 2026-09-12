package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AccessDecisionInternalWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.InternalAccessDecisionInteractor;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Segundo adaptador primario de {@code AuthorizeUseCase} (HU-003, decisión D1): canal interno para
 * el PEP, protegido por la cadena de seguridad de {@code /internal/v1/**} (mTLS + evidencia JWT),
 * separada de la cadena BFF que protege {@link AuthorizationController}.
 *
 * <p>La respuesta **no** se envuelve en {@code ApiResponse} (decisión D6): el PEP espera el objeto
 * plano de {@code DecisionAcceso v1} en la raíz del cuerpo.
 */
@RestController
@RequestMapping("/internal/v1/access-decisions")
final class InternalAccessDecisionController {

    private final InternalAccessDecisionInteractor interactor;

    InternalAccessDecisionController(InternalAccessDecisionInteractor interactor) {
        this.interactor = Objects.requireNonNull(interactor, RequiredArgumentMessages.INTERNAL_ACCESS_DECISION_INTERACTOR);
    }

    @PostMapping
    Mono<ResponseEntity<AccessDecisionInternalWebResponse>> evaluate(@RequestBody AccessDecisionRawRequest body) {
        return interactor.execute(body).map(ResponseEntity::ok);
    }
}
