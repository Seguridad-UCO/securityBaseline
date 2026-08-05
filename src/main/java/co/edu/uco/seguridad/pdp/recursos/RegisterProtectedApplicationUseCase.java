package co.edu.uco.seguridad.pdp.recursos;
import reactor.core.publisher.Mono;
public interface RegisterProtectedApplicationUseCase { Mono<ProtectedApplicationCatalogEntry> register(RegisterProtectedApplicationCommand command); }
