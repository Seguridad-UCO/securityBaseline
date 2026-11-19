package co.edu.uco.seguridad.pep.starter.m2m;

import reactor.core.publisher.Mono;

/**
 * Entrega un Bearer token de una identidad de servicio para una dependencia saliente.
 * La aplicación nunca guarda ni renueva tokens: la librería los reutiliza hasta antes de expirar.
 */
public interface ServiceAccessTokenProvider {
    Mono<String> token();
}
