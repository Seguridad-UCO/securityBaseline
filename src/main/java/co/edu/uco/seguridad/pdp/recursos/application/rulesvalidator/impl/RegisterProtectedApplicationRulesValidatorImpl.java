package co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.impl;

import co.edu.uco.seguridad.pdp.recursos.application.rule.ProtectedResourceMustBeUniqueRule;
import co.edu.uco.seguridad.pdp.recursos.application.rule.ProtectedResourceMustBelongToApplicationTenantRule;
import co.edu.uco.seguridad.pdp.recursos.application.rule.ProtectedResourceRegistration;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.RegisterProtectedApplicationRulesValidator;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Ejecuta primero la regla que no necesita repositorio, así un desajuste de inquilino se rechaza sin consulta.
 */
public final class RegisterProtectedApplicationRulesValidatorImpl
        implements RegisterProtectedApplicationRulesValidator {

    private final ProtectedResourceMustBelongToApplicationTenantRule resourceMustBelongToApplicationTenant;
    private final ProtectedResourceMustBeUniqueRule resourceMustBeUnique;

    public RegisterProtectedApplicationRulesValidatorImpl(
            ProtectedResourceMustBelongToApplicationTenantRule resourceMustBelongToApplicationTenant,
            ProtectedResourceMustBeUniqueRule resourceMustBeUnique) {
        this.resourceMustBelongToApplicationTenant = Objects.requireNonNull(resourceMustBelongToApplicationTenant);
        this.resourceMustBeUnique = Objects.requireNonNull(resourceMustBeUnique);
    }

    @Override
    public Mono<Void> execute(ProtectedResourceRegistration registration) {
        return Mono.fromRunnable(() -> resourceMustBelongToApplicationTenant.execute(registration))
                .then(resourceMustBeUnique.execute(registration));
    }
}
