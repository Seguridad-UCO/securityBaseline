package co.edu.uco.seguridad.shared.port;

/**
 * Hashea un secreto para que nunca se persista en claro (HU-012), y compara un secreto en claro
 * contra un hash guardado (HU-013). Un caso de uso nunca instancia un algoritmo de hashing en línea:
 * lo recibe por este puerto, igual que la hora o un identificador.
 */
public interface CredentialHasher {

    String hash(String plaintext);

    boolean matches(String plaintext, String hash);
}
