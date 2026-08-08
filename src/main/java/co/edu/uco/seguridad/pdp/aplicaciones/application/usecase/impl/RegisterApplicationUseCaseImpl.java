package co.edu.uco.seguridad.pdp.aplicaciones.application.usecase.impl;

import co.edu.uco.seguridad.pdp.aplicaciones.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.secondary.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.aplicaciones.application.rulesvalidator.RegisterApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.aplicaciones.domain.Application;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.shared.observability.ReactiveLogContext;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link RegisterApplicationUseCase}: valida reglas, construye la entidad,
 * la persiste y proyecta el resultado.
 *
 * <p>No contiene ninguna regla propia; la lógica de decisión está encapsulada en el
 * {@link RegisterApplicationRulesValidator} que se inyecta como dependencia.</p>
 */
public final class RegisterApplicationUseCaseImpl implements RegisterApplicationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterApplicationUseCaseImpl.class);

    private final RegisterApplicationRulesValidator rules;
    private final ApplicationRepository repository;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public RegisterApplicationUseCaseImpl(RegisterApplicationRulesValidator rules,
                                          ApplicationRepository repository,
                                          IdentifierGenerator identifiers,
                                          TimeProvider time) {
        this.rules = Objects.requireNonNull(rules, "se requiere validador de reglas");
        this.repository = Objects.requireNonNull(repository, "se requiere repositorio de aplicación");
        this.identifiers = Objects.requireNonNull(identifiers, "se requiere generador de identificadores");
        this.time = Objects.requireNonNull(time, "se requiere proveedor de tiempo");
    }

    @Override
    public Mono<RegisteredApplicationResponse> execute(RegisterApplicationRequest dto) {
        return rules.execute(dto)
                .then(Mono.fromSupplier(() -> Application.register(
                        new ApplicationId(identifiers.next()), dto.tenantId(), dto.name(), time.now())))
                .flatMap(repository::save)
                .map(RegisterApplicationUseCaseImpl::toRegistered)
                .transform(ReactiveLogContext.withContext(LOG, "application.register"));
    }

    private static RegisteredApplicationResponse toRegistered(Application application) {
        return new RegisteredApplicationResponse(
                application.id(), application.tenantId(), application.name(), application.registeredAt());
    }
}
