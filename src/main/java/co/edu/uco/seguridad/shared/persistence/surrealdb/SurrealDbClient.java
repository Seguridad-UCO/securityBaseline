package co.edu.uco.seguridad.shared.persistence.surrealdb;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cliente reactivo mínimo para el endpoint HTTP {@code /sql} de SurrealDB (ADR-0004).
 *
 * <p>No hay un driver Java viable: el oficial (`com.surrealdb:surrealdb`) empaqueta el motor
 * embebido compilado para doce plataformas — 212&nbsp;MB, pensado para uso embebido/móvil, no para
 * un cliente delgado hacia un servidor remoto — y el driver "remoto" anterior
 * (`com.surrealdb:surrealdb-driver`) está abandonado desde 2023 en la versión 0.1.0. SurrealQL sobre
 * HTTP con el {@link WebClient} que el proyecto ya tiene (por {@code spring-boot-starter-webflux})
 * es más simple, totalmente reactivo, y no añade una sola dependencia nueva.</p>
 *
 * <p>Cada valor variable de una sentencia se ata como parámetro de query (<code>?campo=valor</code> →
 * <code>$campo</code> en la sentencia), nunca por concatenación de texto — así SurrealDB decide qué
 * es dato y qué es sintaxis, exactamente como lo haría un {@code PreparedStatement}.</p>
 */
public final class SurrealDbClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final SurrealDbProperties properties;

    public SurrealDbClient(WebClient webClient, ObjectMapper objectMapper, SurrealDbProperties properties) {
        this.webClient = Objects.requireNonNull(webClient, "se requiere el WebClient de SurrealDB");
        this.objectMapper = Objects.requireNonNull(objectMapper, "se requiere ObjectMapper");
        this.properties = Objects.requireNonNull(properties, "se requiere la configuración de SurrealDB");
    }

    /**
     * Crea el namespace y la database configurados si todavía no existen. Cada módulo la llama antes
     * de definir sus propias tablas, en vez de depender del orden de arranque entre módulos — es
     * idempotente, así que llamarla varias veces no tiene costo más allá de una sentencia extra.
     */
    public Mono<Void> ensureNamespaceAndDatabase() {
        return execute(
                        "DEFINE NAMESPACE IF NOT EXISTS %s; DEFINE DATABASE IF NOT EXISTS %s;"
                                .formatted(properties.namespace(), properties.database()),
                        Map.of())
                .then();
    }

    /**
     * Ejecuta una o más sentencias SurrealQL (separadas por {@code ;}, opcionalmente envueltas en
     * {@code BEGIN TRANSACTION ... COMMIT TRANSACTION}) y devuelve el campo {@code result} de cada
     * una, en orden.
     *
     * @throws SurrealDbException si la petición completa no pudo interpretarse, o si cualquier
     *                            sentencia individual terminó en {@code status: "ERR"}
     */
    public Mono<List<JsonNode>> execute(String surql, Map<String, String> params) {
        return webClient.post()
                .uri(uriBuilder -> {
                    uriBuilder.path("/sql");
                    params.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(surql)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseStatements);
    }

    private List<JsonNode> parseStatements(String responseBody) {
        JsonNode root = objectMapper.readTree(responseBody);
        if (!root.isArray()) {
            throw new SurrealDbException(root.path("information").asString(responseBody));
        }
        List<JsonNode> results = new ArrayList<>(root.size());
        for (JsonNode statement : root) {
            if (!"OK".equals(statement.path("status").asString())) {
                throw new SurrealDbException(statement.path("result").asString());
            }
            results.add(statement.path("result"));
        }
        return results;
    }
}
