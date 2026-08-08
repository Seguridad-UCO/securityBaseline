package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * Nivel uno de la estrategia de entrada en dos niveles: el JSON exactamente como llegó.
 *
 * <p>Cada campo es un {@code String} y no lleva ninguna anotación de validación a propósito. El enlace
 * por lo tanto siempre tiene éxito, y un valor incorrecto llega a nuestro código en lugar de ser rechazado
 * por el framework antes de que podamos describir qué estaba mal.</p>
 *
 * <p>No lleva {@code tenantId}: desde ADR-0003 el tenant sale del token autenticado, nunca del
 * cuerpo — un campo aquí solo invitaría a un cliente a intentar registrar en un tenant que no es el
 * suyo.</p>
 *
 * <p>Es inerte: no hace ninguna promesa sobre su contenido, por lo que nada puede consumirlo directamente.
 * El interactor es el único camino hacia adelante.</p>
 */
public record RegisterProtectedApplicationRawRequest(String applicationName,
                                                     String resourceCode,
                                                     String action) {
}
