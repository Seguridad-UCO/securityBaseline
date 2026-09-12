package co.edu.uco.seguridad.pdp.assignments.domain.model;

import co.edu.uco.seguridad.pdp.assignments.domain.exception.InvalidValidityException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Intervalo de vigencia de una asignación: inicio y fin opcional. Sin fin significa "vigente
 * indefinidamente"; revocar es fijar el fin al instante de la revocación (RB-05, INV-ASN-01).
 */
public record Validity(Instant validFrom, Optional<Instant> validUntil) {

    public Validity {
        Objects.requireNonNull(validFrom, RequiredArgumentMessages.VALID_FROM);
        Objects.requireNonNull(validUntil, RequiredArgumentMessages.VALIDITY);
        if (validUntil.isPresent() && !validUntil.get().isAfter(validFrom)) {
            throw new InvalidValidityException(ValueObjectMessages.Validity.INVALID_RANGE);
        }
    }

    public static Validity startingNow(Instant now) {
        return new Validity(now, Optional.empty());
    }

    public boolean isActiveAt(Instant instant) {
        return !validFrom.isAfter(instant) && validUntil.map(end -> end.isAfter(instant)).orElse(true);
    }

    public Validity endingAt(Instant instant) {
        return new Validity(validFrom, Optional.of(instant));
    }
}
