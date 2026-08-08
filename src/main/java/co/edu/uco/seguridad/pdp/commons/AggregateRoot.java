package co.edu.uco.seguridad.pdp.commons;

import co.edu.uco.seguridad.shared.event.DomainEvent;

import java.util.List;
import java.util.Objects;

/**
 * Empareja una entidad recién construida con los eventos de dominio que su construcción produjo.
 *
 * <p>A diferencia de un agregado mutable que acumula eventos con {@code registerEvent(...)}, las
 * entidades de esta línea base son records inmutables sin estado oculto (ver 21. Modelo refinado):
 * añadir un campo de eventos dentro de {@code Application} o {@code ProtectedResource} cambiaría su
 * igualdad estructural, y una instancia reconstruida desde almacenamiento dejaría de ser igual a una
 * recién registrada aunque representen el mismo hecho de negocio (ADR-0002).
 *
 * <p>En su lugar, la fábrica que registra la entidad (p. ej. {@code Application.registerWithEvent})
 * devuelve este par: la entidad, para persistir, y los eventos, para publicar. La entidad en sí no
 * sabe que fue envuelta.</p>
 *
 * @param entity       la entidad ya construida
 * @param domainEvents los eventos que produjo esa construcción, en el orden en que deben publicarse
 */
public record AggregateRoot<T, E extends DomainEvent>(T entity, List<E> domainEvents) {

    public AggregateRoot {
        Objects.requireNonNull(entity, "se requiere la entidad del agregado");
        Objects.requireNonNull(domainEvents, "se requiere la lista de eventos de dominio");
    }

    public static <T, E extends DomainEvent> AggregateRoot<T, E> of(T entity, E event) {
        Objects.requireNonNull(event, "se requiere el evento de dominio");
        return new AggregateRoot<>(entity, List.of(event));
    }
}
