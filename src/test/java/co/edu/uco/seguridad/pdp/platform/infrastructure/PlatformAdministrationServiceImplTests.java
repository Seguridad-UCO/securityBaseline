package co.edu.uco.seguridad.pdp.platform.infrastructure;

import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformAdministrationServiceImplTests {
    private final PlatformAdministrationServiceImpl service = new PlatformAdministrationServiceImpl(mock(SurrealDbClient.class));

    @Test
    void rejects_invalid_application_and_url_inputs_before_persistence() {
        assertThatThrownBy(() -> service.createApplication("universidad-uco", "ab", "Descripción", "https://ok.test"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createApplication("universidad-uco", "Horarios", "Descripción", "not-a-url"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("URL base");
    }

    @Test
    void rejects_invalid_resource_methods_paths_and_tenant_inputs_before_persistence() {
        assertThatThrownBy(() -> service.createResource("universidad-uco", "app", "empleados", "GET"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("path");
        assertThatThrownBy(() -> service.createResource("universidad-uco", "app", "/empleados//historial", "GET"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("path");
        assertThatThrownBy(() -> service.createResource("universidad-uco", "app", "/empleados", "TRACE").block())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Método HTTP");
        assertThatThrownBy(() -> service.createTenant("MAYUSCULAS", "Tenant").block())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("minúsculas");
        assertThatThrownBy(() -> service.createTenant("tenant-valido", "ab").block())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("al menos 3");
    }

    @Test
    void queries_each_catalog_using_the_current_tenant_scope() {
        SurrealDbClient database = mock(SurrealDbClient.class);
        when(database.execute(any(), any())).thenReturn(Mono.just(List.of(new ObjectMapper().createArrayNode())));
        var catalog = new PlatformAdministrationServiceImpl(database);

        assertThat(catalog.applications("universidad-uco").block()).isEmpty();
        assertThat(catalog.resources("universidad-uco", "application-1").block()).isEmpty();
        assertThat(catalog.tenants().block()).isEmpty();
        assertThat(catalog.users().block()).isEmpty();
    }

    @Test
    void creates_application_resource_and_tenant_from_database_results() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var empty = mapper.createArrayNode();
        var application = mapper.readTree("[{\"id\":\"application:`app-1`\",\"tenantId\":\"universidad-uco\",\"name\":\"Gestión horaria\",\"description\":\"Horario institucional\",\"baseUrl\":\"https://horarios.uco.edu\",\"registeredAt\":\"2026-08-19T00:00:00Z\"}]");
        SurrealDbClient applicationDb = mock(SurrealDbClient.class);
        when(applicationDb.execute(anyString(), anyMap()))
                .thenReturn(Mono.just(List.of(empty)), Mono.just(List.of(empty, application)));

        var createdApplication = new PlatformAdministrationServiceImpl(applicationDb)
                .createApplication("universidad-uco", "Gestión horaria", "Horario institucional", "https://horarios.uco.edu").block();

        assertThat(createdApplication).extracting("id", "name", "baseUrl")
                .containsExactly("app-1", "Gestión horaria", "https://horarios.uco.edu");

        var existingApplication = mapper.readTree("[{\"id\":\"application:`app-1`\"}]");
        var resource = mapper.readTree("[{\"id\":\"protected_resource:`resource-1`\",\"applicationId\":\"app-1\",\"path\":\"/empleados/{employeeId}/historial/\",\"method\":\"GET\",\"registeredAt\":\"2026-08-19T00:00:00Z\"}]");
        SurrealDbClient resourceDb = mock(SurrealDbClient.class);
        when(resourceDb.execute(anyString(), anyMap())).thenReturn(
                Mono.just(List.of(existingApplication)), Mono.just(List.of(empty)), Mono.just(List.of(empty, resource)));

        var createdResource = new PlatformAdministrationServiceImpl(resourceDb)
                .createResource("universidad-uco", "app-1", "/empleados/{employeeId}/historial/", "get").block();

        assertThat(createdResource).extracting("id", "applicationId", "path", "method")
                .containsExactly("resource-1", "app-1", "/empleados/{employeeId}/historial/", "GET");

        var tenant = mapper.readTree("[{\"id\":\"tenant:estudiantes-uco\",\"name\":\"Estudiantes UCO\",\"status\":\"ACTIVE\"}]");
        SurrealDbClient tenantDb = mock(SurrealDbClient.class);
        when(tenantDb.execute(anyString(), anyMap())).thenReturn(Mono.just(List.of(empty)), Mono.just(List.of(empty, tenant)));

        var createdTenant = new PlatformAdministrationServiceImpl(tenantDb)
                .createTenant("estudiantes-uco", "Estudiantes UCO").block();

        assertThat(createdTenant).extracting("code", "name", "status")
                .containsExactly("estudiantes-uco", "Estudiantes UCO", "ACTIVE");
    }

    @Test
    void provisions_new_identity_and_changes_a_users_tenant() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var empty = mapper.createArrayNode();
        var user = mapper.readTree("[{\"id\":\"security_user:`user-1`\",\"tenantId\":\"universidad-uco\",\"email\":\"david@uco.edu\",\"name\":\"David Alzate\",\"createdAt\":\"2026-08-19T00:00:00Z\",\"lastLoginAt\":\"2026-08-19T00:00:00Z\"}]");
        SurrealDbClient provisionDb = mock(SurrealDbClient.class);
        when(provisionDb.execute(anyString(), anyMap())).thenReturn(
                Mono.just(List.of(empty)), Mono.just(List.of(empty)), Mono.just(List.of(empty)),
                Mono.just(List.of(empty)), Mono.just(List.of(user)));

        var principal = new PlatformAdministrationServiceImpl(provisionDb)
                .provision("https://accounts.google.com", "google-subject", "David@UCO.edu", "David Alzate", "google").block();

        assertThat(principal.userId()).isNotBlank();
        assertThat(principal.subject()).isEqualTo("google-subject");
        assertThat(principal.tenantId().value()).isEqualTo("universidad-uco");

        var activeTenant = mapper.readTree("[{\"id\":\"tenant:estudiantes-uco\"}]");
        var reassigned = mapper.readTree("[{\"id\":\"security_user:`user-1`\",\"email\":\"david@uco.edu\",\"name\":\"David Alzate\",\"tenantId\":\"estudiantes-uco\",\"createdAt\":\"2026-08-19T00:00:00Z\",\"lastLoginAt\":\"2026-08-19T00:00:00Z\"}]");
        SurrealDbClient assignmentDb = mock(SurrealDbClient.class);
        when(assignmentDb.execute(anyString(), anyMap())).thenReturn(Mono.just(List.of(activeTenant)), Mono.just(List.of(empty, reassigned)));

        var assigned = new PlatformAdministrationServiceImpl(assignmentDb).assignTenant("user-1", "estudiantes-uco").block();

        assertThat(assigned).extracting("id", "tenantId").containsExactly("user-1", "estudiantes-uco");
    }

    @Test
    void reuses_a_known_identity_and_updates_the_local_profile_on_repeated_oidc_login() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var identity = mapper.readTree("[{\"userId\":\"user-1\",\"subject\":\"oidc-subject\"}]");
        var updatedUser = mapper.readTree("[{\"tenantId\":\"universidad-uco\",\"email\":\"david@uco.edu\",\"name\":\"David desde Keycloak\"}]");
        SurrealDbClient database = mock(SurrealDbClient.class);
        when(database.execute(anyString(), anyMap()))
                .thenReturn(Mono.just(List.of(identity)), Mono.just(List.of(mapper.createArrayNode(), updatedUser)));

        var principal = new PlatformAdministrationServiceImpl(database)
                .provision("http://localhost:9090/realms/security-baseline", "oidc-subject", "david@uco.edu", "David desde Keycloak", "keycloak-local").block();

        assertThat(principal).extracting("userId", "subject", "tenantId.value", "name", "email")
                .containsExactly("user-1", "oidc-subject", "universidad-uco", "David desde Keycloak", "david@uco.edu");
    }

    @Test
    void rejects_duplicate_or_foreign_catalog_records_and_loads_a_known_identity() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var empty = mapper.createArrayNode();
        var existing = mapper.readTree("[{\"id\":\"application:`app-1`\"}]");
        SurrealDbClient duplicateApplicationDb = mock(SurrealDbClient.class);
        when(duplicateApplicationDb.execute(anyString(), anyMap())).thenReturn(Mono.just(List.of(existing)));
        assertThatThrownBy(() -> new PlatformAdministrationServiceImpl(duplicateApplicationDb)
                .createApplication("universidad-uco", "Horarios", "Gestión", "https://horarios.uco.edu").block())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Ya existe");

        SurrealDbClient foreignApplicationDb = mock(SurrealDbClient.class);
        when(foreignApplicationDb.execute(anyString(), anyMap())).thenReturn(Mono.just(List.of(empty)));
        assertThatThrownBy(() -> new PlatformAdministrationServiceImpl(foreignApplicationDb)
                .createResource("universidad-uco", "app-1", "/empleados", "GET").block())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no existe");

        var duplicateResource = mapper.readTree("[{\"id\":\"protected_resource:`resource-1`\"}]");
        SurrealDbClient duplicateResourceDb = mock(SurrealDbClient.class);
        when(duplicateResourceDb.execute(anyString(), anyMap())).thenReturn(Mono.just(List.of(existing)), Mono.just(List.of(duplicateResource)));
        assertThatThrownBy(() -> new PlatformAdministrationServiceImpl(duplicateResourceDb)
                .createResource("universidad-uco", "app-1", "/empleados", "GET").block())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ya está registrado");

        SurrealDbClient duplicateTenantDb = mock(SurrealDbClient.class);
        when(duplicateTenantDb.execute(anyString(), anyMap())).thenReturn(Mono.just(List.of(existing)));
        assertThatThrownBy(() -> new PlatformAdministrationServiceImpl(duplicateTenantDb)
                .createTenant("estudiantes-uco", "Estudiantes UCO").block())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ya existe");

        SurrealDbClient inactiveTenantDb = mock(SurrealDbClient.class);
        when(inactiveTenantDb.execute(anyString(), anyMap())).thenReturn(Mono.just(List.of(empty)));
        assertThatThrownBy(() -> new PlatformAdministrationServiceImpl(inactiveTenantDb)
                .assignTenant("user-1", "inactivo").block())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no existe o está inactivo");

        var identity = mapper.readTree("[{\"userId\":\"user-1\",\"subject\":\"google-subject\"}]");
        var knownUser = mapper.readTree("[{\"tenantId\":\"universidad-uco\",\"email\":\"david@uco.edu\",\"name\":\"David Alzate\"}]");
        SurrealDbClient knownIdentityDb = mock(SurrealDbClient.class);
        when(knownIdentityDb.execute(anyString(), anyMap())).thenReturn(Mono.just(List.of(identity)), Mono.just(List.of(empty)), Mono.just(List.of(empty, knownUser)));

        var known = new PlatformAdministrationServiceImpl(knownIdentityDb)
                .provision("https://accounts.google.com", "google-subject", "david@uco.edu", "David Alzate", "google").block();
        assertThat(known.userId()).isEqualTo("user-1");
    }
}
