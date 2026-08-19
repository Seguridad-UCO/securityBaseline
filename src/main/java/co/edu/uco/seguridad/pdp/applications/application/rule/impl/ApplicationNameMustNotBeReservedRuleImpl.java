package co.edu.uco.seguridad.pdp.applications.application.rule.impl;

import co.edu.uco.seguridad.pdp.applications.application.exception.ReservedApplicationNameException;
import co.edu.uco.seguridad.pdp.applications.application.rule.ApplicationNameMustNotBeReservedRule;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ApplicationNameMustNotBeReservedRuleImpl implements ApplicationNameMustNotBeReservedRule {

    private final Set<String> reservedNames;

    public ApplicationNameMustNotBeReservedRuleImpl(Set<String> reservedNames) {
        this.reservedNames = Objects.requireNonNull(reservedNames, RequiredArgumentMessages.RESERVED_NAMES).stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void execute(ApplicationName name) {
        if (reservedNames.contains(name.value().toLowerCase(Locale.ROOT))) {
            throw new ReservedApplicationNameException(name);
        }
    }
}
