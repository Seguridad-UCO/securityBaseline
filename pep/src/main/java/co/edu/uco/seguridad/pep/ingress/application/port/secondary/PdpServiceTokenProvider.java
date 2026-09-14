package co.edu.uco.seguridad.pep.ingress.application.port.secondary;

import reactor.core.publisher.Mono;

/** Obtiene la identidad técnica con la que el PEP llama al canal interno del PDP. */
public interface PdpServiceTokenProvider {
    Mono<String> token();
}
