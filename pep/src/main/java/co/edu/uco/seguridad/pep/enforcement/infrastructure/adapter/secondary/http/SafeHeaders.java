package co.edu.uco.seguridad.pep.enforcement.infrastructure.adapter.secondary.http;

import org.springframework.http.HttpHeaders;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Trust-boundary sanitation applied in both directions, including Connection-nominated fields.
 */
final class SafeHeaders {
    private static final Set<String> HOP = Set.of("connection", "keep-alive", "proxy-authenticate",
            "proxy-authorization", "te", "trailer", "transfer-encoding", "upgrade", "host");
    private static final Set<String> REQUEST_ALLOWED = Set.of("accept", "accept-encoding", "accept-language",
            "content-type", "content-length", "content-encoding", "content-language", "content-disposition",
            "range", "if-range", "if-match", "if-none-match", "if-modified-since", "if-unmodified-since",
            "cache-control", "pragma", "user-agent", "origin", "idempotency-key", "authorization", "cookie");

    private SafeHeaders() {
    }

    static void copy(HttpHeaders source, HttpHeaders target, boolean request, boolean bearer, boolean cookies) {
        Set<String> blocked = new HashSet<>(HOP);
        for (String value : source.getOrEmpty("Connection")) {
            for (String name : value.split(",")) blocked.add(name.trim().toLowerCase(Locale.ROOT));
        }
        source.forEach((name, values) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            if ((request && !REQUEST_ALLOWED.contains(lower)) || blocked.contains(lower)
                    || lower.startsWith("x-forwarded-") || lower.equals("forwarded")
                    || lower.startsWith("x-pep-") || lower.startsWith("x-security-")
                    || lower.startsWith("x-user") || lower.startsWith("x-tenant") || lower.startsWith("x-role")
                    || lower.startsWith("x-profile") || lower.startsWith("x-subject")
                    || lower.equals("x-authenticated-user") || lower.equals("remote-user")
                    || lower.equals("x-request-id") || lower.equals("x-correlation-id") || lower.equals("x-decision-id")
                    || lower.startsWith("access-control-")
                    || (request && ((!bearer && lower.equals("authorization"))
                    || (!cookies && lower.equals("cookie"))))
                    || (!request && lower.equals("set-cookie") && !cookies)) return;
            target.put(name, List.copyOf(values));
        });
    }
}
