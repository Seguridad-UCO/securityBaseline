package co.edu.uco.seguridad.shared.audit;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Fakes compartidos de {@link AdministrationAuditRepository} para los tests de los 15 casos de uso
 * retrofit de HU-021 (§7 PLAN-HU-021).
 */
public final class TestAdministrationAuditRepositories {

    private TestAdministrationAuditRepositories() {
    }

    /**
     * Para pruebas que todavía no ejercitan la auditoría real.
     */
    public static AdministrationAuditRepository unreachable() {
        return new AdministrationAuditRepository() {
            @Override
            public Mono<Void> save(AdministrationEvent event) {
                throw new AssertionError("must not reach AdministrationAuditRepository");
            }

            @Override
            public Flux<AdministrationEvent> findByCorrelationId(String correlationId) {
                throw new AssertionError("must not reach AdministrationAuditRepository");
            }
        };
    }

    /**
     * Guarda cada evento recibido en {@code captured}, sin fallar nunca.
     */
    public static AdministrationAuditRepository capturing(List<AdministrationEvent> captured) {
        return new AdministrationAuditRepository() {
            @Override
            public Mono<Void> save(AdministrationEvent event) {
                captured.add(event);
                return Mono.empty();
            }

            @Override
            public Flux<AdministrationEvent> findByCorrelationId(String correlationId) {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Siempre falla al guardar — para probar que un fallo de auditoría no bloquea el resultado.
     */
    public static AdministrationAuditRepository failing() {
        return new AdministrationAuditRepository() {
            @Override
            public Mono<Void> save(AdministrationEvent event) {
                return Mono.error(new RuntimeException("no se pudo registrar la auditoría"));
            }

            @Override
            public Flux<AdministrationEvent> findByCorrelationId(String correlationId) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
