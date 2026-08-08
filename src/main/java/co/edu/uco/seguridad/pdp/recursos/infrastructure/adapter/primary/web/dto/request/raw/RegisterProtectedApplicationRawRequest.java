package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Nivel uno de la estrategia de entrada en dos niveles: el JSON exactamente como llegó.
 *
 * <p>Cada campo es un {@code String} y no lleva ninguna anotación de validación a propósito. El enlace
 * por lo tanto siempre tiene éxito, y un valor incorrecto llega a nuestro código en lugar de ser rechazado
 * por el framework antes de que podamos describir qué estaba mal.</p>
 *
 * <p>Es inerte: no hace ninguna promesa sobre su contenido, por lo que nada puede consumirlo directamente.
 * El interactor es el único camino hacia adelante.</p>
 */
public record RegisterProtectedApplicationRawRequest(String tenantId,
                                                     String applicationName,
                                                     String resourceCode,
                                                     String action) {
}
