package co.edu.uco.seguridad.shared.event;

import java.time.Instant;

/**
 * Un hecho de negocio ya ocurrido, publicado para que otros módulos reaccionen sin que quien lo
 * produce sepa quién escucha.
 *
 * <p>Contrato técnico, no vocabulario del negocio — por eso vive en {@code shared}, junto a los demás
 * puertos transversales, y no en {@code pdp/commons}. Cada evento concreto (p. ej.
 * {@code ApplicationRegistered}, {@code ProtectedResourceRegistered}) es un {@code record} del módulo
 * que lo produce.</p>
 */
public interface DomainEvent {

    Instant occurredOn();
}
