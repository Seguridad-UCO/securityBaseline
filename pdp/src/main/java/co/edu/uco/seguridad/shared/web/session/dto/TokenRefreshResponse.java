package co.edu.uco.seguridad.shared.web.session.dto;

public record TokenRefreshResponse(String accessToken, long expiresIn, String tokenType) {
    public TokenRefreshResponse {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken es obligatorio");
        }
        if (expiresIn <= 0) {
            throw new IllegalArgumentException("expiresIn debe ser positivo");
        }
        if (tokenType == null || tokenType.isBlank()) {
            throw new IllegalArgumentException("tokenType es obligatorio");
        }
    }
}
