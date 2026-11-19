package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.shared.port.CredentialHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hashea la credencial de una aplicación con SHA-256 (SU-02).
 *
 * <p><strong>Por qué no BCrypt.</strong> BCrypt es lento a propósito para que una contraseña humana,
 * de poca entropía, no se pueda adivinar probando. La credencial de una aplicación no es eso: son 32
 * bytes de {@code SecureRandom} (256 bits), imposibles de recorrer por fuerza bruta con cualquier
 * hash. Con BCrypt cada validación costaba ~70 ms de CPU sobre el hilo de eventos, y el starter
 * valida la credencial en <em>cada</em> decisión: con 50 decisiones simultáneas eso agotaba el timeout
 * del PEP (hallazgo SU-02 de Asistencias UCO). SHA-256 cuesta microsegundos.</p>
 *
 * <p><strong>Compatibilidad.</strong> Los hashes BCrypt existentes ({@code $2…}) se siguen aceptando;
 * {@link #needsRehash(String)} los señala para que la siguiente validación correcta los reescriba. La
 * comparación de SHA-256 es en tiempo constante ({@link MessageDigest#isEqual}).</p>
 */
public final class Sha256CredentialHasher implements CredentialHasher {

    static final String PREFIX = "sha256:";
    private static final HexFormat HEX = HexFormat.of();

    private final BCryptPasswordEncoder legacy = new BCryptPasswordEncoder();

    @Override
    public String hash(String plaintext) {
        return PREFIX + HEX.formatHex(digest(plaintext));
    }

    @Override
    public boolean matches(String plaintext, String hash) {
        if (plaintext == null || hash == null) {
            return false;
        }
        if (!hash.startsWith(PREFIX)) {
            return legacy.matches(plaintext, hash);
        }
        try {
            return MessageDigest.isEqual(digest(plaintext), HEX.parseHex(hash.substring(PREFIX.length())));
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    @Override
    public boolean needsRehash(String hash) {
        return hash == null || !hash.startsWith(PREFIX);
    }

    private static byte[] digest(String plaintext) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(plaintext.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException(unavailable);
        }
    }
}
