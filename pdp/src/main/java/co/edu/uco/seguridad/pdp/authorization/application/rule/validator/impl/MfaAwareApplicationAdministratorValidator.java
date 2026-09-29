package co.edu.uco.seguridad.pdp.authorization.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.MfaEvidenceRequiredException;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.mfa.MfaEvidenceProperties;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Decora {@link PrincipalMustBeApplicationAdministratorValidator} con el step-up de MFA (HU-024,
 * ADR-027) — mismo patrón que {@code RevocationAwareJwtDecoder} en HU-022: envuelve el único
 * contrato que los 12 {@code Administer*UseCaseImpl} ya inyectan, así que ninguno de ellos cambia.
 *
 * <p>Pendiente: {@code delegate.execute(input)} primero (autorización de rol vía OPA); solo si
 * permite, evalúa {@code mfaProperties.satisfiedBy(input.authenticationContext())} y, si no
 * satisface, falla con {@link MfaEvidenceRequiredException}. Nunca evalúa MFA si el delegado ya
 * rechazó — PLAN-HU-024.md §1.2.</p>
 */
public final class MfaAwareApplicationAdministratorValidator implements PrincipalMustBeApplicationAdministratorValidator {

    private final PrincipalMustBeApplicationAdministratorValidator delegate;
    private final MfaEvidenceProperties mfaProperties;

    public MfaAwareApplicationAdministratorValidator(PrincipalMustBeApplicationAdministratorValidator delegate,
                                                     MfaEvidenceProperties mfaProperties) {
        this.delegate = Objects.requireNonNull(delegate, RequiredArgumentMessages.MFA_AWARE_VALIDATOR_DELEGATE);
        this.mfaProperties = Objects.requireNonNull(mfaProperties, RequiredArgumentMessages.MFA_EVIDENCE_PROPERTIES);
    }

    @Override
    public Mono<Void> execute(AdministrationRequest input) {
        return delegate.execute(input)
                .then(Mono.defer(() -> mfaProperties.satisfiedBy(input.authenticationContext())
                        ? Mono.<Void>empty()
                        : Mono.error(new MfaEvidenceRequiredException(input.tenantId(), input.applicationId()))));
    }
}
