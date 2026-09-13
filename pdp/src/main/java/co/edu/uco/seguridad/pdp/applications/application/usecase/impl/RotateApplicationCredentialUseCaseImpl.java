package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RotateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RotateApplicationCredentialUseCase;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationExistence;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.CredentialHasher;
import co.edu.uco.seguridad.shared.port.SecretGenerator;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link RotateApplicationCredentialUseCase} (HU-014). Pendiente: resolver la
 * aplicación con {@code findByIdForTenant} (vacío → invoca {@link ApplicationMustExistForTenantRule}
 * con {@code registered=false}, que siempre lanza), generar el secreto, hashearlo,
 * {@code updateCredentialHash} y devolver la proyección de la aplicación (identidad intacta) con el
 * secreto nuevo en claro.
 */
public final class RotateApplicationCredentialUseCaseImpl implements RotateApplicationCredentialUseCase {

    private final ApplicationRepository repository;
    private final ApplicationMustExistForTenantRule mustExist;
    private final SecretGenerator secretGenerator;
    private final CredentialHasher hasher;

    public RotateApplicationCredentialUseCaseImpl(ApplicationRepository repository,
            ApplicationMustExistForTenantRule mustExist, SecretGenerator secretGenerator, CredentialHasher hasher) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.APPLICATION_REPOSITORY);
        this.mustExist = Objects.requireNonNull(mustExist, RequiredArgumentMessages.APPLICATION_EXISTS_RULE);
        this.secretGenerator = Objects.requireNonNull(secretGenerator, RequiredArgumentMessages.SECRET_GENERATOR);
        this.hasher = Objects.requireNonNull(hasher, RequiredArgumentMessages.CREDENTIAL_HASHER);
    }

    @Override
    public Mono<ApplicationRegistrationResponse> execute(RotateApplicationCredentialRequest input) {
        return repository.findByIdForTenant(input.tenantId(), input.applicationId())
                .doOnNext(application -> mustExist.execute(
                        new ApplicationExistence(input.tenantId(), input.applicationId(), true)))
                .switchIfEmpty(Mono.fromRunnable(() -> mustExist.execute(
                        new ApplicationExistence(input.tenantId(), input.applicationId(), false))))
                .flatMap(application -> {
                    String secret = secretGenerator.next();
                    ApplicationCredentialHash credentialHash = new ApplicationCredentialHash(hasher.hash(secret));
                    Application rotated = application.withCredentialHash(credentialHash);
                    return repository.updateCredentialHash(rotated.id(), credentialHash)
                            .thenReturn(new ApplicationRegistrationResponse(toRegistered(rotated), secret));
                });
    }

    private static RegisteredApplicationResponse toRegistered(Application application) {
        return new RegisteredApplicationResponse(application.id(), application.tenantId(), application.name(),
                application.description(), application.baseUrl(), application.registeredAt());
    }
}
