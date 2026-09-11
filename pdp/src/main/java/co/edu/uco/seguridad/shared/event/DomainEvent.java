package co.edu.uco.seguridad.shared.event;

import java.time.Instant;

/**
 * Un hecho de negocio ya ocurrido, publicado para que otros módulos reaccionen sin que quien lo
 * produce sepa quién escucha. Contrato técnico, no vocabulario del negocio — por eso vive en
 * {@code shared} y no en {@code pdp/commons}.
 */
public interface DomainEvent {

    Instant occurredOn();
}
