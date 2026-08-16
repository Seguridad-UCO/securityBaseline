package co.edu.uco.seguridad.pdp.commons;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.event.DomainEvent;

import java.util.List;
import java.util.Objects;

/**
 * Empareja una entidad recién construida con los eventos que su construcción produjo. Existe porque
 * las entidades son records inmutables: un campo de eventos dentro de {@code Application} cambiaría
 * su igualdad estructural (ver ADR-017).
 */
public record AggregateRoot<T, E extends DomainEvent>(T entity, List<E> domainEvents) {

    public AggregateRoot {
        Objects.requireNonNull(entity, RequiredArgumentMessages.AGGREGATE_ENTITY);
        Objects.requireNonNull(domainEvents, RequiredArgumentMessages.DOMAIN_EVENTS_LIST);
    }

    public static <T, E extends DomainEvent> AggregateRoot<T, E> of(T entity, E event) {
        Objects.requireNonNull(event, RequiredArgumentMessages.DOMAIN_EVENT);
        return new AggregateRoot<>(entity, List.of(event));
    }
}
