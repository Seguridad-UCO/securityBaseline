package co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.impl;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.pdp.recursos.application.model.ProtectedResourceRegistration;
import co.edu.uco.seguridad.pdp.recursos.application.rule.ProtectedResourceMustBeUniqueRule;
import co.edu.uco.seguridad.pdp.recursos.application.rule.ProtectedResourceMustBelongToApplicationTenantRule;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.RegisterProtectedApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Ejecuta primero las reglas que no necesitan repositorio, así un desajuste de inquilino se rechaza
 * sin consulta. El chequeo de inquilino activo se declara explícitamente acá — no basta con que
 * {@code aplicaciones} ya lo valide al registrar la aplicación: un tenant suspendido no debe poder
 * registrar un recurso protegido tampoco, y ese invariante debe sostenerse por una regla propia de
 * este módulo, no por un efecto colateral de otro.
 */
public final class RegisterProtectedApplicationRulesValidatorImpl
        implements RegisterProtectedApplicationRulesValidator {

    private final TenantMustBeActiveRule tenantMustBeActive;
    private final ProtectedResourceMustBelongToApplicationTenantRule resourceMustBelongToApplicationTenant;
    private final ProtectedResourceMustBeUniqueRule resourceMustBeUnique;

    public RegisterProtectedApplicationRulesValidatorImpl(
            TenantMustBeActiveRule tenantMustBeActive,
            ProtectedResourceMustBelongToApplicationTenantRule resourceMustBelongToApplicationTenant,
            ProtectedResourceMustBeUniqueRule resourceMustBeUnique) {
        this.tenantMustBeActive = Objects.requireNonNull(tenantMustBeActive, RequiredArgumentMessages.TENANT_RULE);
        this.resourceMustBelongToApplicationTenant = Objects.requireNonNull(resourceMustBelongToApplicationTenant);
        this.resourceMustBeUnique = Objects.requireNonNull(resourceMustBeUnique);
    }

    @Override
    public Mono<Void> execute(ProtectedResourceRegistration registration) {
        return Mono.fromRunnable(() -> resourceMustBelongToApplicationTenant.execute(registration))
                .then(tenantMustBeActive.execute(registration.dto().tenantId()))
                .then(resourceMustBeUnique.execute(registration));
    }
}
