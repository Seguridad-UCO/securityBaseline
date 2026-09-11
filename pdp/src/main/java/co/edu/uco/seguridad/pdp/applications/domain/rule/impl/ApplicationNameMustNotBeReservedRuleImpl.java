package co.edu.uco.seguridad.pdp.applications.domain.rule.impl;

import co.edu.uco.seguridad.pdp.applications.domain.exception.ReservedApplicationNameException;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationNameMustNotBeReservedRule;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * La lista reservada llega por constructor desde la configuración del módulo. Sigue siendo una
 * decisión pura: mismo conjunto y mismo nombre, misma respuesta, sin E/S de por medio.
 */
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
