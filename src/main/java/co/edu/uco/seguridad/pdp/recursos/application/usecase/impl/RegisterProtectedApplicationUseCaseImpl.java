package co.edu.uco.seguridad.pdp.recursos.application.usecase.impl;

import co.edu.uco.seguridad.pdp.aplicaciones.ApplicationsModuleApi;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.mapper.ProtectedResourceCatalogMapper;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.RegisterProtectedApplicationUseCase;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.recursos.application.port.secondary.AuditPort;
import co.edu.uco.seguridad.pdp.recursos.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.recursos.application.rule.ProtectedResourceRegistration;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.RegisterProtectedApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedResource;
import co.edu.uco.seguridad.shared.observability.ReactiveLogContext;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.ReactiveTransactionPort;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Orquesta E-1: registrar la aplicación a través de Aplicaciones, luego su primer recurso aquí,
 * como una unidad de trabajo.
 *
 * <p>Dos mecanismos de recuperación, porque hay dos almacenes. {@code ReactiveTransactionPort} revierte
 * las propias escrituras de este módulo; eliminar la aplicación es una compensación explícita, ya que
 * vive detrás del límite de otro módulo y no puede unirse a esta transacción. Nombrar la compensación
 * en lugar de ocultarla mantiene la saga visible para quien reemplace los dummies con infraestructura real.</p>
 */
public final class RegisterProtectedApplicationUseCaseImpl implements RegisterProtectedApplicationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterProtectedApplicationUseCaseImpl.class);

    private final ApplicationsModuleApi applications;
    private final RegisterProtectedApplicationRulesValidator rules;
    private final ProtectedResourceRepository resources;
    private final AuditPort audit;
    private final ReactiveTransactionPort transaction;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public RegisterProtectedApplicationUseCaseImpl(ApplicationsModuleApi applications,
                                                    RegisterProtectedApplicationRulesValidator rules,
                                                    ProtectedResourceRepository resources,
                                                    AuditPort audit,
                                                    ReactiveTransactionPort transaction,
                                                    IdentifierGenerator identifiers,
                                                    TimeProvider time) {
        this.applications = Objects.requireNonNull(applications, "se requiere API del módulo de aplicaciones");
        this.rules = Objects.requireNonNull(rules, "se requiere validador de reglas");
        this.resources = Objects.requireNonNull(resources, "se requiere repositorio de recurso protegido");
        this.audit = Objects.requireNonNull(audit, "se requiere puerto de auditoría");
        this.transaction = Objects.requireNonNull(transaction, "se requiere puerto de transacción");
        this.identifiers = Objects.requireNonNull(identifiers, "se requiere generador de identificadores");
        this.time = Objects.requireNonNull(time, "se requiere proveedor de tiempo");
    }

    @Override
    public Mono<ProtectedApplicationResponse> execute(RegisterProtectedApplicationRequest dto) {
        return transaction.execute(() -> registerApplication(dto)
                        .flatMap(application -> registerResource(dto, application)
                                .onErrorResume(error -> removeApplication(application).then(Mono.error(error)))))
                .transform(ReactiveLogContext.withContext(LOG, "protected_application.register"));
    }

    private Mono<RegisteredApplicationResponse> registerApplication(RegisterProtectedApplicationRequest dto) {
        return applications.register(new RegisterApplicationRequest(dto.tenantId(), dto.applicationName()));
    }

    private Mono<ProtectedApplicationResponse> registerResource(RegisterProtectedApplicationRequest dto,
                                                           RegisteredApplicationResponse application) {
        ProtectedResourceRegistration registration = new ProtectedResourceRegistration(dto, application);
        return rules.execute(registration)
                .then(Mono.fromSupplier(() -> buildResource(dto, application)))
                .flatMap(resources::save)
                .flatMap(resource -> audit.protectedApplicationRegistered(resource)
                        .thenReturn(ProtectedResourceCatalogMapper.toResponse(resource)));
    }

    private ProtectedResource buildResource(RegisterProtectedApplicationRequest dto,
                                            RegisteredApplicationResponse application) {
        return ProtectedResource.register(
                new ResourceId(identifiers.next()),
                application.id(),
                dto.tenantId(),
                application.name(),
                dto.resourceCode(),
                dto.action(),
                time.now());
    }

    private Mono<Void> removeApplication(RegisteredApplicationResponse application) {
        return applications.remove(application.id());
    }
}
