package co.edu.uco.seguridad.pdp.platform.application;

import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import reactor.core.publisher.Mono;

import java.util.List;

/** Puerto de aplicación para el catálogo operativo y el aprovisionamiento de identidad. */
public interface PlatformAdministrationService {
    record ApplicationView(String id, String name, String description, String baseUrl, String tenantId, String registeredAt) { }
    record ResourceView(String id, String applicationId, String path, String method, String registeredAt) { }
    record TenantView(String code, String name, String status) { }
    record UserView(String id, String email, String name, String provider, String tenantId, String createdAt, String lastLoginAt) { }
    Mono<LocalUserPrincipal> provision(String issuer, String subject, String email, String name, String provider);
    Mono<List<ApplicationView>> applications(String tenantId);
    Mono<ApplicationView> createApplication(String tenantId, String name, String description, String baseUrl);
    Mono<List<ResourceView>> resources(String tenantId, String applicationId);
    Mono<ResourceView> createResource(String tenantId, String applicationId, String path, String method);
    Mono<List<TenantView>> tenants();
    Mono<TenantView> createTenant(String code, String name);
    Mono<List<UserView>> users();
    Mono<UserView> assignTenant(String userId, String tenantCode);
}
