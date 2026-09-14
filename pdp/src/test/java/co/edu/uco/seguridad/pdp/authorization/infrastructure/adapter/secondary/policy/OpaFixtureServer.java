package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sustituto embebido del API HTTP de OPA (mismo patrón que el servidor JWKS de
 * {@code InternalSecurityChainIntegrationTests}): sin Docker, sin dependencia nueva. Por defecto
 * responde lo que respondería OPA hoy, sin políticas de aplicación publicadas — DENY/NO_APPLICABLE_POLICY —
 * y {@link #respondWith(int, String)} lo cambia para un caso concreto.
 *
 * <p><b>Respuesta por ruta (HU-017):</b> el PDP consulta a OPA por dos rutas distintas —
 * {@code pdp.opa.decision-path} (autorización de negocio) y
 * {@code pdp.opa.administration-decision-path} (¿administra esta aplicación?, HU-009/HU-015/HU-016/HU-017)
 * — y una sola clase de prueba puede necesitar respuestas distintas para cada una: por ejemplo,
 * {@code AuthorizationHttpTests} deja el DENY por defecto para sus propias pruebas de autorización,
 * pero su *fixture* de {@code @BeforeEach} necesita que la decisión de administración sea ALLOW para
 * poder registrar la aplicación/el recurso de cada prueba. {@link #respondWithForPath(String, int, String)}
 * fija una respuesta solo para esa ruta, sin tocar el default que seguirá usando cualquier otra ruta
 * (incluida la de autorización, si nadie la fijó aparte).</p>
 */
public final class OpaFixtureServer {

    private static final String DEFAULT_BODY = """
            {"result":{"effect":"DENY","reasonCode":"NO_APPLICABLE_POLICY","policyReferences":[],"obligations":[]}}""";

    private record Response(int status, String body) {
    }

    private final HttpServer server;
    private final Map<String, Response> pathResponses = new ConcurrentHashMap<>();
    private volatile Response defaultResponse = new Response(200, DEFAULT_BODY);
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
        Response response = pathResponses.getOrDefault(exchange.getRequestURI().getPath(), defaultResponse);
        byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(response.status(), bytes.length);
        try (var responseBody = exchange.getResponseBody()) {
            responseBody.write(bytes);
        }
    }

    /** Cambia la respuesta por defecto — la que usa cualquier ruta sin una fijada por su cuenta. */
    public void respondWith(int status, String jsonBody) {
        this.defaultResponse = new Response(status, jsonBody);
        this.delayMillis = 0;
    }

    /**
     * Cambia la respuesta solo para una ruta exacta (ej. {@code pdp.opa.administration-decision-path}),
     * sin afectar el default ni ninguna otra ruta.
     */
    public void respondWithForPath(String path, int status, String jsonBody) {
        pathResponses.put(path, new Response(status, jsonBody));
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
