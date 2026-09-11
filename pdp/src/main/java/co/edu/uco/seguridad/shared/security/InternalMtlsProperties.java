package co.edu.uco.seguridad.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Confianza mTLS del canal interno (HU-003, decisión D2, confirmada con el usuario: PEM + lista de
 * sujetos, sin keystore). {@code trustCertificate} es la ruta al CA en PEM que
 * {@code server.ssl.trust-certificate} usa para validar la cadena del certificado de cliente en el
 * handshake (Netty ya rechaza una cadena no confiable antes de que llegue al filtro).
 * {@code allowedSubjects} es la lista de Subject DN/CN exactos admitidos — vacía por defecto, así
 * que ningún certificado pasa hasta que se configure explícitamente (falla cerrado).
 */
@ConfigurationProperties(prefix = "pdp.security.internal.mtls")
public record InternalMtlsProperties(String trustCertificate, List<String> allowedSubjects) {

    public InternalMtlsProperties {
    }
}
