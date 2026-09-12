package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Sustituto embebido del API HTTP de OPA (mismo patrón que el servidor JWKS de
 * {@code InternalSecurityChainIntegrationTests}): sin Docker, sin dependencia nueva. Por defecto
 * responde lo que respondería OPA hoy, sin políticas de aplicación publicadas — DENY/NO_APPLICABLE_POLICY —
 * y {@link #respondWith(int, String)} lo cambia para un caso concreto.
 */
public final class OpaFixtureServer {

    private static final String DEFAULT_BODY = """
            {"result":{"effect":"DENY","reasonCode":"NO_APPLICABLE_POLICY","policyReferences":[],"obligations":[]}}""";

    private final HttpServer server;
    private volatile int status = 200;
    private volatile String body = DEFAULT_BODY;
    private volatile long delayMillis = 0;
    private volatile String lastRequestBody = "";

    private OpaFixtureServer(HttpServer server) {
        this.server = server;
    }

    public static OpaFixtureServer start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        OpaFixtureServer fixture = new OpaFixtureServer(server);
        server.createContext("/", fixture::handle);
        server.start();
        return fixture;
    }

    private void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        try (InputStream requestBody = exchange.getRequestBody()) {
            lastRequestBody = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var responseBody = exchange.getResponseBody()) {
            responseBody.write(bytes);
        }
    }

    public void respondWith(int status, String jsonBody) {
        this.status = status;
        this.body = jsonBody;
        this.delayMillis = 0;
    }

    public void respondAfterDelay(long millis) {
        this.delayMillis = millis;
    }

    public String lastRequestBody() {
        return lastRequestBody;
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void stop() {
        server.stop(0);
    }
}
