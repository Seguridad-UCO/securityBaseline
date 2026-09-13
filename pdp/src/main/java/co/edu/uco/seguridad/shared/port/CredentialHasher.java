package co.edu.uco.seguridad.shared.port;

/**
 * Hashea un secreto para que nunca se persista en claro (HU-012). Un caso de uso nunca instancia un
 * algoritmo de hashing en línea: lo recibe por este puerto, igual que la hora o un identificador.
 */
@FunctionalInterface
public interface CredentialHasher {

    String hash(String plaintext);
}
