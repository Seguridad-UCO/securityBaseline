package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse.BulkResourceRowError;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedResourcesInBulkUseCase;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Modalidad {@code resources-roles}: un solo archivo con las hojas Recursos, Roles y RolesxRecurso.
 *
 * <p>No reimplementa reglas: compone las dos cargas existentes. Los recursos se validan con
 * {@link BulkResourceImportService} (tenant, gate de administración de la aplicación y duplicados) y los roles con
 * {@link BulkCatalogImportService} (gates ADR-023/ADR-036 y duplicados); RolesxRecurso admite como destino tanto
 * recursos persistidos como los que la hoja Recursos va a crear.</p>
 *
 * <p>Todo o nada: si cualquier hoja o relación tiene errores no se persiste nada. Al confirmar se crean primero los
 * recursos y después los roles con sus vínculos; si los roles fallan, los recursos recién creados se eliminan
 * (compensación explícita, mismo patrón que HU-010) y el error se propaga.</p>
 */
public final class BulkResourceRoleImportService implements BulkCatalogImport {
    private static final String RESOURCES_SHEET = "Recursos";
    private static final String FILE_FIELD = "file";

    private final BulkResourceImportService resourceImports;
    private final BulkCatalogImportService catalogImports;
    private final RegisterProtectedResourcesInBulkUseCase bulkRegistration;
    private final ProtectedResourceRepository resources;
    private final RoleRepository roles;

    public BulkResourceRoleImportService(BulkResourceImportService resourceImports, BulkCatalogImportService catalogImports,
                                         RegisterProtectedResourcesInBulkUseCase bulkRegistration,
                                         ProtectedResourceRepository resources, RoleRepository roles) {
        this.resourceImports = Objects.requireNonNull(resourceImports);
        this.catalogImports = Objects.requireNonNull(catalogImports);
        this.bulkRegistration = Objects.requireNonNull(bulkRegistration);
        this.resources = Objects.requireNonNull(resources);
        this.roles = Objects.requireNonNull(roles);
    }

    @Override
    public Mono<BulkResourceValidationResponse> validate(byte[] bytes, PdpPrincipal principal, UserId userId) {
        return resolve(bytes, principal, userId).map(Result::response);
    }

    @Override
    public Mono<BulkResourceValidationResponse> commit(byte[] bytes, PdpPrincipal principal, UserId userId) {
        return resolve(bytes, principal, userId).flatMap(result -> {
            if (!result.response().ready()) return Mono.just(result.response());
            return persist(result).thenReturn(result.response());
        });
    }

    /**
     * Valida las tres hojas en orden: primero Recursos (para conocer los recursos nuevos), luego Roles y RolesxRecurso.
     */
    Mono<Result> resolve(byte[] bytes, PdpPrincipal principal, UserId userId) {
        return resourceImports.validate(bytes, principal, userId).flatMap(resourceResult -> {
            Map<BulkCatalogImportService.PendingResource, ResourceId> pending = new HashMap<>();
            for (ProtectedResource resource : resourceResult.resources()) {
                pending.put(new BulkCatalogImportService.PendingResource(resource.applicationId(), resource.path(), resource.method()), resource.id());
            }
            return catalogImports.validateRoles(bytes, principal, userId, pending)
                    .map(roleResult -> new Result(merge(resourceResult.response(), roleResult.response()), resourceResult.resources(), roleResult.roles()));
        });
    }

    private Mono<Void> persist(Result result) {
        List<ProtectedResource> created = result.resources();
        return bulkRegistration.execute(created)
                .then(Mono.defer(() -> roles.saveAllAtomically(result.roles())
                        .onErrorResume(failure -> compensate(created, failure).then(Mono.error(failure)))));
    }

    /**
     * Elimina los recursos creados por esta carga; un fallo aquí se adjunta al error original, que es el que se reporta.
     */
    private Mono<Void> compensate(List<ProtectedResource> created, Throwable cause) {
        return Flux.fromIterable(created)
                .concatMap(resource -> resources.deleteById(resource.id()))
                .then()
                .onErrorResume(compensationFailure -> {
                    cause.addSuppressed(compensationFailure);
                    return Mono.empty();
                });
    }

    /**
     * Une los resultados de las dos validaciones. Los errores de la hoja Recursos se rotulan con la hoja para no
     * confundir sus filas con las de Roles; los errores de archivo (vacío, ilegible, cifrado) se reportan una vez.
     */
    private static BulkResourceValidationResponse merge(BulkResourceValidationResponse resourcesResponse, BulkResourceValidationResponse rolesResponse) {
        List<BulkResourceRowError> errors = new ArrayList<>();
        for (BulkResourceRowError error : resourcesResponse.errors()) {
            if (isFileError(error)) {
                boolean alreadyReported = rolesResponse.errors().stream().anyMatch(other -> isFileError(other) && other.code().equals(error.code()));
                if (!alreadyReported) errors.add(error);
                continue;
            }
            errors.add(new BulkResourceRowError(error.row(), RESOURCES_SHEET + " · " + error.field(), error.value(), error.code(), error.description()));
        }
        errors.addAll(rolesResponse.errors());
        return new BulkResourceValidationResponse(errors.isEmpty(),
                resourcesResponse.totalRows() + rolesResponse.totalRows(),
                resourcesResponse.validRows() + rolesResponse.validRows(),
                resourcesResponse.invalidRows() + rolesResponse.invalidRows(),
                List.copyOf(errors));
    }

    private static boolean isFileError(BulkResourceRowError error) {
        return error.row() == 0 && FILE_FIELD.equals(error.field());
    }

    record Result(BulkResourceValidationResponse response, List<ProtectedResource> resources, List<Role> roles) {
    }
}
