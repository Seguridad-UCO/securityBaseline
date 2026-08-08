package co.edu.uco.seguridad.shared.persistence.surrealdb;

/**
 * SurrealDB devuelve el campo {@code id} de una fila como {@code tabla:valor}, y entre comillas
 * invertidas cuando el valor no es un identificador simple (cualquier UUID o slug con guiones cae
 * aquí) — p. ej. {@code tenant:`universidad-uco`}. Cada adaptador necesita el valor desnudo para
 * reconstruir el value object de dominio; esta clase centraliza ese único formato en vez de que cada
 * mapper repita su propio recorte de cadena.
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
