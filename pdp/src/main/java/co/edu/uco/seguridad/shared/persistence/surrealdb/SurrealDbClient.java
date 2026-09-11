package co.edu.uco.seguridad.shared.persistence.surrealdb;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
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
 * Cliente reactivo mínimo para el endpoint HTTP {@code /sql} de SurrealDB — ver ADR-019 para por qué
 * no hay driver Java. Cada valor variable se ata como parámetro de query, nunca por concatenación,
 * para que SurrealDB decida qué es dato y qué es sintaxis.
 */
public final class SurrealDbClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final SurrealDbProperties properties;

    public SurrealDbClient(WebClient webClient, ObjectMapper objectMapper, SurrealDbProperties properties) {
        this.webClient = Objects.requireNonNull(webClient, RequiredArgumentMessages.SURREALDB_WEBCLIENT);
        this.objectMapper = Objects.requireNonNull(objectMapper, RequiredArgumentMessages.OBJECT_MAPPER);
        this.properties = Objects.requireNonNull(properties, RequiredArgumentMessages.SURREALDB_PROPERTIES);
    }

    /** Idempotente — cada módulo la llama antes de definir sus tablas, sin depender del orden de arranque. */
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
