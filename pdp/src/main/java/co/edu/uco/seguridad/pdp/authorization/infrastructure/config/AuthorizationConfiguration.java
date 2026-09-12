package co.edu.uco.seguridad.pdp.authorization.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.PolicyDecisionPort;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AuthorizeUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.EvaluateInternalAccessUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AuthorizeUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.EvaluateInternalAccessUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AuthorizeInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.InternalAccessDecisionInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.AuthorizeInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.InternalAccessDecisionInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.DenyByDefaultPolicyDecisionAdapter;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceMustExistValidator;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.observability.ReactiveTelemetry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** La unica clase consciente de Spring del modulo. */
@Configuration
public class AuthorizationConfiguration {

    @Bean
    PolicyDecisionPort policyDecisionPort(IdentifierGenerator identifiers, TimeProvider time) {
        return new DenyByDefaultPolicyDecisionAdapter(identifiers, time);
    }

    @Bean
    AuthorizeUseCase authorizeUseCase(ApplicationMustExistForTenantValidator applicationMustExist,
            ProtectedResourceMustExistValidator resourceMustExist, PolicyDecisionPort policyDecisionPort,
            IdentifierGenerator identifiers, TimeProvider time, ObservationRegistry observations) {
        var delegate = new AuthorizeUseCaseImpl(applicationMustExist, resourceMustExist, policyDecisionPort, identifiers, time);
        return input -> ReactiveTelemetry.observe("security.authorization", observations,
                io.micrometer.common.KeyValues.of("decision", "none", "reason", "none"), () -> delegate.execute(input),
                (observation, decision) -> observation.lowCardinalityKeyValue("decision", decision.state().name())
                        .lowCardinalityKeyValue("reason", decision.reasonCode().name()));
    }

    @Bean
    AuthorizeInteractor authorizeInteractor(AuthorizeUseCase useCase) {
        return new AuthorizeInteractorImpl(useCase);
    }

    // HU-003 — canal interno para el PEP (D1: mismo AuthorizeUseCase, segundo adaptador primario).
    @Bean
    EvaluateInternalAccessUseCase evaluateInternalAccessUseCase(ApplicationOwnerLookupValidator ownerLookup,
            AuthorizeUseCase authorizeUseCase, IdentifierGenerator identifiers, TimeProvider time) {
        return new EvaluateInternalAccessUseCaseImpl(ownerLookup, authorizeUseCase, identifiers, time);
    }

    @Bean
    InternalAccessDecisionInteractor internalAccessDecisionInteractor(EvaluateInternalAccessUseCase useCase) {
        return new InternalAccessDecisionInteractorImpl(useCase);
    }
}
