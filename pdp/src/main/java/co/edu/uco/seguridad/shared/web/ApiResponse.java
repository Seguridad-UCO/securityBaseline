package co.edu.uco.seguridad.shared.web;

import java.time.Instant;

public record ApiResponse<T>(String code, String message, T data, Instant timestamp, String requestId,
                             String correlationId) {
    public static <T> ApiResponse<T> success(String code, String message, T data, RequestContext context) {
        return new ApiResponse<>(code, message, data, Instant.now(), context.requestId(), context.correlationId());
    }
}
