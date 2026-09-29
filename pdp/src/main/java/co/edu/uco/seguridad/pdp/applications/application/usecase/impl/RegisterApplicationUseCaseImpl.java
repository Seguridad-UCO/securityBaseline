package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.RegisterApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.observability.ReactiveLogContext;
import co.edu.uco.seguridad.shared.port.CredentialHasher;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.SecretGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link RegisterApplicationUseCase}: valida reglas, construye la entidad y la
 * persiste. Sin reglas propias — la decisión vive en {@link RegisterApplicationRulesValidator}.
 *
 * <p>{@code execute} pendiente de HU-012: generar el secreto con {@link SecretGenerator}, hashearlo
 * con {@link CredentialHasher}, construir la entidad con el hash y devolver el secreto en claro
 * dentro de {@link ApplicationRegistrationResponse} — solo esta vez.</p>
 */
public final class RegisterApplicationUseCaseImpl implements RegisterApplicationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterApplicationUseCaseImpl.class);

    private final RegisterApplicationRulesValidator rules;
    private final ApplicationRepository repository;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;
    private final SecretGenerator secretGenerator;
    private final CredentialHasher hasher;

    public RegisterApplicationUseCaseImpl(RegisterApplicationRulesValidator rules,
                                          ApplicationRepository repository, IdentifierGenerator identifiers, TimeProvider time,
                                          SecretGenerator secretGenerator, CredentialHasher hasher) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.RULES_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.APPLICATION_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
        this.secretGenerator = Objects.requireNonNull(secretGenerator, RequiredArgumentMessages.SECRET_GENERATOR);
        this.hasher = Objects.requireNonNull(hasher, RequiredArgumentMessages.CREDENTIAL_HASHER);
    }

    @Override
    public Mono<ApplicationRegistrationResponse> execute(RegisterApplicationRequest dto) {
        return rules.execute(dto)
                .then(Mono.fromSupplier(() -> {
                    String secret = secretGenerator.next();
                    ApplicationCredentialHash credentialHash = new ApplicationCredentialHash(hasher.hash(secret));
                    Application application = Application.register(new ApplicationId(identifiers.next()),
                            dto.tenantId(), dto.name(), dto.description(), dto.baseUrl(), credentialHash, time.now());
                    return new PendingRegistration(application, secret);
                }))
                .flatMap(pending -> repository.save(pending.application())
                        .map(saved -> new ApplicationRegistrationResponse(toRegistered(saved), pending.secret())))
                .transform(ReactiveLogContext.withContext(LOG, "application.register"));
    }

    private static RegisteredApplicationResponse toRegistered(Application application) {
        return new RegisteredApplicationResponse(application.id(), application.tenantId(), application.name(),
                application.description(), application.baseUrl(), application.registeredAt());
    }

    private record PendingRegistration(Application application, String secret) {
    }
}
