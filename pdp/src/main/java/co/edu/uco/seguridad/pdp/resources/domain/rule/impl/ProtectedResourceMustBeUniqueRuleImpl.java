package co.edu.uco.seguridad.pdp.resources.domain.rule.impl;

import co.edu.uco.seguridad.pdp.resources.domain.exception.DuplicateProtectedResourceException;
import co.edu.uco.seguridad.pdp.resources.domain.rule.ProtectedResourceMustBeUniqueRule;
import co.edu.uco.seguridad.pdp.resources.domain.rule.model.ProtectedResourceAvailability;

public final class ProtectedResourceMustBeUniqueRuleImpl implements ProtectedResourceMustBeUniqueRule {

    @Override
    public void execute(ProtectedResourceAvailability availability) {
        if (availability.alreadyRegistered()) {
            throw new DuplicateProtectedResourceException(availability.path(), availability.method());
        }
    }
}
