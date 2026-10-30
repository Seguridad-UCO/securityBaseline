package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationNameForTenantQuery;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationNameForTenantLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.domain.Profile;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScopeLevel;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Importa catálogos sin relaciones: RolesxRecurso y PerfilesxRol pueden quedar vacías.
 */
public final class BulkCatalogImportService {
    private final BulkCatalogWorkbookService books;
    private final RoleRepository roles;
    private final ProfileRepository profiles;
    private final ProtectedResourceRepository resources;
    private final ApplicationNameForTenantLookupValidator applications;
    private final IdentifierGenerator ids;
    private final TimeProvider time;

    public BulkCatalogImportService(BulkCatalogWorkbookService books, RoleRepository roles, ProfileRepository profiles, ProtectedResourceRepository resources, ApplicationNameForTenantLookupValidator applications, IdentifierGenerator ids, TimeProvider time) {
        this.books = books;
        this.roles = roles;
        this.profiles = profiles;
        this.resources = resources;
        this.applications = applications;
        this.ids = ids;
        this.time = time;
    }

    public Mono<Result> validate(String mode, byte[] bytes, TenantId tenant) {
        List<BulkResourceValidationResponse.BulkResourceRowError> errors = new ArrayList<>();
        List<BulkCatalogWorkbookService.CatalogRow> rows;
        try {
            rows = books.parse(mode, bytes);
        } catch (Exception e) {
            return Mono.just(new Result(response(0, errors(e.getMessage())), List.of(), List.of()));
        }
        Set<String> seen = new HashSet<>();
        List<Entry> accepted = new ArrayList<>();
        return Flux.fromIterable(rows).concatMap(row -> resolve(mode, row, tenant, seen, errors).doOnNext(accepted::add)).then(mode.equals("roles") ? resolveRelations(bytes, tenant, accepted, errors) : resolveProfileRelations(bytes, tenant, accepted, errors)).then(Mono.fromSupplier(() -> result(rows.size(), errors, accepted)));
    }

    public Mono<Result> commit(String mode, byte[] bytes, TenantId tenant) {
        return validate(mode, bytes, tenant).flatMap(result -> {
            if (!result.response().ready()) return Mono.just(result);
            return mode.equals("roles") ? roles.saveAllAtomically(result.roles()).thenReturn(result) : profiles.saveAllAtomically(result.profiles()).thenReturn(result);
        });
    }

    private Mono<Entry> resolve(String mode, BulkCatalogWorkbookService.CatalogRow row, TenantId tenant, Set<String> seen, List<BulkResourceValidationResponse.BulkResourceRowError> errors) {
        try {
            return scope(row, tenant).flatMap(scope -> {
                String key = row.name().toLowerCase(Locale.ROOT) + "|" + scope;
                if (!seen.add(key)) {
                    errors.add(error(row.row(), "Nombre", "DUPLICATE_IN_FILE", "El nombre se repite en el mismo alcance"));
                    return Mono.empty();
                }
                if (mode.equals("roles")) {
                    RoleName name = new RoleName(row.name());
                    return roles.existsByNameInScope(name, scope).flatMap(exists -> {
                        if (exists) {
                            errors.add(error(row.row(), "Nombre de rol*", "DUPLICATE_PERSISTED", "El rol ya existe"));
                            return Mono.empty();
                        }
                        return Mono.just(new Entry(row, scope, name.value()));
                    });
                }
                ProfileName name = new ProfileName(row.name());
                return profiles.existsByNameInScope(name, scope).flatMap(exists -> {
                    if (exists) {
                        errors.add(error(row.row(), "Nombre de perfil*", "DUPLICATE_PERSISTED", "El perfil ya existe"));
                        return Mono.empty();
                    }
                    return Mono.just(new Entry(row, scope, name.value()));
                });
            }).onErrorResume(e -> {
                errors.add(error(row.row(), "Aplicación", "INVALID_VALUE", e.getMessage()));
                return Mono.empty();
            });
        } catch (Exception e) {
            errors.add(error(row.row(), "row", "INVALID_VALUE", e.getMessage()));
            return Mono.empty();
        }
    }

    private Mono<RoleScope> scope(BulkCatalogWorkbookService.CatalogRow row, TenantId tenant) {
        RoleScopeLevel level = RoleScopeLevel.parse(row.scope());
        if (level == RoleScopeLevel.TENANT) {
            if (!row.applicationId().isBlank())
                throw new IllegalArgumentException("Aplicación solo aplica al alcance APPLICATION");
            return Mono.just(RoleScope.ofTenant(tenant));
        }
        if (level != RoleScopeLevel.APPLICATION || row.applicationId().isBlank())
            throw new IllegalArgumentException("APPLICATION requiere Aplicación");
        return applications.execute(new ApplicationNameForTenantQuery(tenant, new ApplicationName(row.applicationId()))).map(application -> RoleScope.ofApplication(tenant, application.id())).switchIfEmpty(Mono.defer(() -> Mono.error(new IllegalArgumentException("La aplicación no pertenece al tenant autenticado"))));
    }

    private Mono<Void> resolveRelations(byte[] bytes, TenantId tenant, List<Entry> entries, List<BulkResourceValidationResponse.BulkResourceRowError> errors) {
        final List<BulkCatalogWorkbookService.RoleResourceRow> rows;
        try {
            rows = books.parseRoleResources(bytes);
        } catch (Exception e) {
            errors.add(error(0, "RolesxRecurso", "INVALID_WORKBOOK", e.getMessage()));
            return Mono.empty();
        }
        Set<String> seen = new HashSet<>();
        return Flux.fromIterable(rows).concatMap(row -> {
            String key = row.role() + "|" + row.scope() + "|" + row.application() + "|" + row.path() + "|" + row.method();
            if (!seen.add(key)) {
                errors.add(error(row.row(), "RolesxRecurso", "DUPLICATE_IN_FILE", "La relación rol-recurso está repetida"));
                return Mono.empty();
            }
            return relation(row, tenant, entries, errors);
        }).then();
    }

    private Mono<Void> relation(BulkCatalogWorkbookService.RoleResourceRow row, TenantId tenant, List<Entry> entries, List<BulkResourceValidationResponse.BulkResourceRowError> errors) {
        try {
            return applications.execute(new ApplicationNameForTenantQuery(tenant, new ApplicationName(row.application()))).switchIfEmpty(Mono.error(new IllegalArgumentException("La aplicación no pertenece al tenant autenticado"))).flatMap(app -> {
                RoleScope scope = RoleScopeLevel.parse(row.scope()) == RoleScopeLevel.TENANT ? RoleScope.ofTenant(tenant) : RoleScope.ofApplication(tenant, app.id());
                Entry entry = entries.stream().filter(e -> e.name().equalsIgnoreCase(row.role()) && e.scope().equals(scope)).findFirst().orElse(null);
                if (entry == null) {
                    errors.add(error(row.row(), "Nombre de rol*", "ROLE_NOT_FOUND", "El rol debe existir en la hoja Roles para recibir recursos"));
                    return Mono.empty();
                }
                return resources.findIdByApplicationPathAndMethod(app.id(), new ResourcePath(row.path()), HttpVerb.parse(row.method())).switchIfEmpty(Mono.error(new IllegalArgumentException("El recurso no existe en la aplicación indicada"))).doOnNext(entry.resources()::add).then();
            }).onErrorResume(e -> {
                errors.add(error(row.row(), "RolesxRecurso", "INVALID_REFERENCE", e.getMessage()));
                return Mono.empty();
            });
        } catch (Exception e) {
            errors.add(error(row.row(), "RolesxRecurso", "INVALID_VALUE", e.getMessage()));
            return Mono.empty();
        }
    }

    private Mono<Void> resolveProfileRelations(byte[] bytes, TenantId tenant, List<Entry> entries, List<BulkResourceValidationResponse.BulkResourceRowError> errors) {
        final List<BulkCatalogWorkbookService.ProfileRoleRow> rows;
        try {
            rows = books.parseProfileRoles(bytes);
        } catch (Exception e) {
            errors.add(error(0, "PerfilesxRol", "INVALID_WORKBOOK", e.getMessage()));
            return Mono.empty();
        }
        Set<String> seen = new HashSet<>();
        return Flux.fromIterable(rows).concatMap(row -> {
            String key = row.profile() + "|" + row.profileScope() + "|" + row.profileApplication() + "|" + row.role() + "|" + row.roleScope() + "|" + row.roleApplication();
            if (!seen.add(key)) {
                errors.add(error(row.row(), "PerfilesxRol", "DUPLICATE_IN_FILE", "La relación perfil-rol está repetida"));
                return Mono.empty();
            }
            return profileRelation(row, tenant, entries, errors);
        }).then();
    }

    private Mono<Void> profileRelation(BulkCatalogWorkbookService.ProfileRoleRow row, TenantId tenant, List<Entry> entries, List<BulkResourceValidationResponse.BulkResourceRowError> errors) {
        try {
            return scope(new BulkCatalogWorkbookService.CatalogRow(row.row(), row.profile(), row.profileScope(), row.profileApplication()), tenant).flatMap(profileScope -> {
                Entry profile = entries.stream().filter(e -> e.name().equalsIgnoreCase(row.profile()) && e.scope().equals(profileScope)).findFirst().orElse(null);
                if (profile == null) {
                    errors.add(error(row.row(), "PerfilesxRol", "PROFILE_NOT_FOUND", "El perfil debe existir en la hoja Perfiles"));
                    return Mono.empty();
                }
                return scope(new BulkCatalogWorkbookService.CatalogRow(row.row(), row.role(), row.roleScope(), row.roleApplication()), tenant).flatMap(roleScope -> roles.findByNameInScope(new RoleName(row.role()), roleScope).switchIfEmpty(Mono.error(new IllegalArgumentException("El rol referenciado no existe"))).doOnNext(role -> profile.roles().add(role.id())).then());
            }).onErrorResume(e -> {
                errors.add(error(row.row(), "PerfilesxRol", "INVALID_REFERENCE", e.getMessage()));
                return Mono.empty();
            });
        } catch (Exception e) {
            errors.add(error(row.row(), "PerfilesxRol", "INVALID_VALUE", e.getMessage()));
            return Mono.empty();
        }
    }

    private Result result(int total, List<BulkResourceValidationResponse.BulkResourceRowError> errors, List<Entry> entries) {
        long invalid = errors.stream().map(BulkResourceValidationResponse.BulkResourceRowError::row).filter(i -> i > 0).distinct().count();
        List<Role> rs = entries.stream().map(e -> new Role(new RoleId(ids.next()), new RoleName(e.name()), e.scope(), e.resources(), time.now())).toList();
        List<Profile> ps = entries.stream().map(e -> new Profile(new ProfileId(ids.next()), new ProfileName(e.name()), e.scope(), e.roles(), time.now())).toList();
        return new Result(response(total, errors, (int) invalid), rs, ps);
    }

    private BulkResourceValidationResponse response(int total, List<BulkResourceValidationResponse.BulkResourceRowError> errors) {
        return response(total, errors, total);
    }

    private BulkResourceValidationResponse response(int total, List<BulkResourceValidationResponse.BulkResourceRowError> errors, int invalid) {
        return new BulkResourceValidationResponse(errors.isEmpty(), total, total - invalid, invalid, List.copyOf(errors));
    }

    private List<BulkResourceValidationResponse.BulkResourceRowError> errors(String message) {
        return List.of(error(0, "file", "INVALID_WORKBOOK", message));
    }

    private static BulkResourceValidationResponse.BulkResourceRowError error(int row, String field, String code, String description) {
        return new BulkResourceValidationResponse.BulkResourceRowError(row, field, "", code, description);
    }

    private record Entry(BulkCatalogWorkbookService.CatalogRow row, RoleScope scope, String name,
                         Set<ResourceId> resources, Set<RoleId> roles) {
        Entry(BulkCatalogWorkbookService.CatalogRow row, RoleScope scope, String name) {
            this(row, scope, name, new HashSet<>(), new HashSet<>());
        }
    }

    public record Result(BulkResourceValidationResponse response, List<Role> roles, List<Profile> profiles) {
    }
}
