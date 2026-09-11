package co.edu.uco.seguridad.shared.persistence.surrealdb;

/**
 * SurrealDB devuelve {@code id} como {@code tabla:valor}, entre comillas invertidas cuando el valor
 * no es un identificador simple — p. ej. {@code tenant:`universidad-uco`}. Centraliza ese recorte en
 * vez de que cada mapper repita el suyo.
 */
public final class SurrealRecordId {

    private SurrealRecordId() {
    }

    public static String idPart(String recordId) {
        String afterColon = recordId.substring(recordId.indexOf(':') + 1);
        if (afterColon.length() >= 2 && afterColon.startsWith("`") && afterColon.endsWith("`")) {
            return afterColon.substring(1, afterColon.length() - 1);
        }
        return afterColon;
    }
}
