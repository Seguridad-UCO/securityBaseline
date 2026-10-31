package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationNameForTenantQuery;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationNameForTenantLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse.BulkResourceRowError;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
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
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Importa catálogos sin relaciones: RolesxRecurso y PerfilesxRol pueden quedar vacías.
 *
 * <p>Quien carga una fila de alcance APPLICATION debe administrar esa aplicación (ADR-023): el gate se consulta una
 * sola vez por aplicación y por llamada, antes de cualquier comprobación de duplicados, y cualquier fallo del gate
 * se reporta como {@code NOT_AUTHORIZED} (fail-closed). La aplicación se busca siempre dentro del tenant del
 * principal, de modo que una aplicación ajena es indistinguible de una inexistente.</p>
 */
public final class BulkCatalogImportService {
    private static final String ROLES_MODE = "roles";
    private static final String PROFILES_MODE = "profiles";
    private static final String ROLE_RESOURCES_SHEET = "RolesxRecurso";
    private static final String PROFILE_ROLES_SHEET = "PerfilesxRol";

    private static final String ROLE_NAME_FIELD = "Nombre de rol*";
    private static final String PROFILE_NAME_FIELD = "Nombre de perfil*";
    private static final String SCOPE_FIELD = "Alcance*";
    private static final String APPLICATION_FIELD = "Aplicación";

    private static final String UNSUPPORTED_MODE_MESSAGE = "Modalidad de carga no soportada: use roles o profiles";
    private static final String APPLICATION_NOT_FOUND_MESSAGE = "No se encontró la aplicación en el tenant autenticado";
    private static final String NOT_AUTHORIZED_MESSAGE = "No está autorizado para administrar esta aplicación";
    private static final String APPLICATION_ONLY_FOR_APPLICATION_SCOPE_MESSAGE = "Aplicación solo aplica al alcance APPLICATION";
    private static final String APPLICATION_SCOPE_REQUIRES_APPLICATION_MESSAGE = "APPLICATION requiere Aplicación";
    private static final String UNSUPPORTED_SCOPE_MESSAGE = "El alcance debe ser TENANT o APPLICATION";
    private static final String ROLE_NAME_REQUIRED_MESSAGE = "El nombre del rol es obligatorio";
    private static final String PROFILE_NAME_REQUIRED_MESSAGE = "El nombre del perfil es obligatorio";
    private static final String SCOPE_REQUIRED_MESSAGE = "El alcance es obligatorio";
    private static final String PROFILE_SCOPE_REQUIRED_MESSAGE = "El alcance del perfil es obligatorio";
    private static final String ROLE_SCOPE_REQUIRED_MESSAGE = "El alcance del rol es obligatorio";
    private static final String APPLICATION_REQUIRED_MESSAGE = "La aplicación es obligatoria";
    private static final String PATH_REQUIRED_MESSAGE = "La ruta es obligatoria";
    private static final String METHOD_REQUIRED_MESSAGE = "El método HTTP es obligatorio";
    private static final String DUPLICATE_NAME_MESSAGE = "El nombre se repite en el mismo alcance";
    private static final String ROLE_EXISTS_MESSAGE = "El rol ya existe";
    private static final String PROFILE_EXISTS_MESSAGE = "El perfil ya existe";
    private static final String ROLE_NOT_IN_SHEET_MESSAGE = "El rol debe existir en la hoja Roles para recibir recursos";
    private static final String PROFILE_NOT_IN_SHEET_MESSAGE = "El perfil debe existir en la hoja Perfiles";
    private static final String RESOURCE_NOT_FOUND_MESSAGE = "El recurso no existe en la aplicación indicada";
    private static final String REFERENCED_ROLE_NOT_FOUND_MESSAGE = "El rol referenciado no existe";
    private static final String DUPLICATE_ROLE_RESOURCE_MESSAGE = "La relación rol-recurso está repetida";
    private static final String DUPLICATE_PROFILE_ROLE_MESSAGE = "La relación perfil-rol está repetida";

    private final BulkCatalogWorkbookService books;
    private final RoleRepository roles;
    private final ProfileRepository profiles;
    private final ProtectedResourceRepository resources;
    private final ApplicationNameForTenantLookupValidator applications;
    private final PrincipalMustBeApplicationAdministratorValidator administrators;
    private final IdentifierGenerator ids;
    private final TimeProvider time;

    public BulkCatalogImportService(BulkCatalogWorkbookService books, RoleRepository roles, ProfileRepository profiles, ProtectedResourceRepository resources, ApplicationNameForTenantLookupValidator applications, PrincipalMustBeApplicationAdministratorValidator administrators, IdentifierGenerator ids, TimeProvider time) {
        this.books = books;
        this.roles = roles;
        this.profiles = profiles;
        this.resources = resources;
        this.applications = applications;
        this.administrators = Objects.requireNonNull(administrators, RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.ids = ids;
        this.time = time;
    }

    public Mono<Result> validate(String mode, byte[] bytes, PdpPrincipal principal, UserId userId) {
        if (!ROLES_MODE.equals(mode) && !PROFILES_MODE.equals(mode)) {
            return Mono.error(new UnsupportedBulkModeException(UNSUPPORTED_MODE_MESSAGE));
        }
        Objects.requireNonNull(userId, RequiredArgumentMessages.USER_ID);
        boolean roleMode = ROLES_MODE.equals(mode);
        return Mono.defer(() -> run(roleMode, bytes, principal, userId));
    }

    public Mono<Result> commit(String mode, byte[] bytes, PdpPrincipal principal, UserId userId) {
        return validate(mode, bytes, principal, userId).flatMap(result -> {
            if (!result.response().ready()) return Mono.just(result);
            return ROLES_MODE.equals(mode) ? roles.saveAllAtomically(result.roles()).thenReturn(result) : profiles.saveAllAtomically(result.profiles()).thenReturn(result);
        });
    }

    private Mono<Result> run(boolean roleMode, byte[] bytes, PdpPrincipal principal, UserId userId) {
        BulkCatalogWorkbookService.Parsed<BulkCatalogWorkbookService.CatalogRow> parsed = books.parse(roleMode ? ROLES_MODE : PROFILES_MODE, bytes);
        if (parsed.rows().isEmpty()) {
            return Mono.just(new Result(response(0, parsed.errors(), List.of()), List.of(), List.of()));
        }
        Batch batch = new Batch(principal, userId, new ArrayList<>(parsed.errors()), new ArrayList<>(), new ArrayList<>(), new HashSet<>(), new HashMap<>(), rowsOf(parsed.errors()));
        return Flux.fromIterable(parsed.rows())
                .concatMap(row -> resolve(roleMode, row, batch))
                .doOnNext(batch.accepted()::add)
                .then(Mono.defer(() -> roleMode ? resolveRoleResources(bytes, batch) : resolveProfileRoles(bytes, batch)))
                .then(Mono.fromSupplier(() -> result(roleMode, parsed.rows().size(), batch)));
    }

    // ---- hoja principal ---------------------------------------------------------------------------------------

    private Mono<Entry> resolve(boolean roleMode, BulkCatalogWorkbookService.CatalogRow row, Batch batch) {
        if (batch.cellErrorRows().contains(row.row())) return Mono.empty();
        String nameField = roleMode ? ROLE_NAME_FIELD : PROFILE_NAME_FIELD;
        List<BulkResourceRowError> errors = batch.errors();

        boolean complete = true;
        if (row.name().isBlank()) {
            errors.add(error(row.row(), nameField, "REQUIRED", roleMode ? ROLE_NAME_REQUIRED_MESSAGE : PROFILE_NAME_REQUIRED_MESSAGE));
            complete = false;
        }
        if (row.scope().isBlank()) {
            errors.add(error(row.row(), SCOPE_FIELD, "REQUIRED", SCOPE_REQUIRED_MESSAGE));
            complete = false;
        }
        if (!complete) return Mono.empty();

        String name = null;
        try {
            name = roleMode ? new RoleName(row.name()).value() : new ProfileName(row.name()).value();
        } catch (RuntimeException invalid) {
            errors.add(error(row.row(), nameField, "INVALID_VALUE", invalid.getMessage()));
        }
        RoleScopeLevel level = null;
        try {
            level = RoleScopeLevel.parse(row.scope());
            if (level == RoleScopeLevel.GLOBAL) {
                errors.add(error(row.row(), SCOPE_FIELD, "INVALID_VALUE", UNSUPPORTED_SCOPE_MESSAGE));
                level = null;
            }
        } catch (RuntimeException invalid) {
            errors.add(error(row.row(), SCOPE_FIELD, "INVALID_VALUE", invalid.getMessage()));
        }
        if (name == null || level == null) return Mono.empty();

        boolean hasApplication = !row.applicationId().isBlank();
        if (level == RoleScopeLevel.TENANT && hasApplication) {
            errors.add(error(row.row(), APPLICATION_FIELD, "INVALID_VALUE", APPLICATION_ONLY_FOR_APPLICATION_SCOPE_MESSAGE));
            return Mono.empty();
        }
        if (level == RoleScopeLevel.APPLICATION && !hasApplication) {
            errors.add(error(row.row(), APPLICATION_FIELD, "INVALID_VALUE", APPLICATION_SCOPE_REQUIRES_APPLICATION_MESSAGE));
            return Mono.empty();
        }

        final String normalizedName = name;
        Mono<RoleScope> scope;
        if (level == RoleScopeLevel.TENANT) {
            scope = Mono.just(RoleScope.ofTenant(batch.principal().tenantId()));
        } else {
            final ApplicationName application;
            try {
                application = new ApplicationName(row.applicationId());
            } catch (RuntimeException invalid) {
                errors.add(error(row.row(), APPLICATION_FIELD, "INVALID_VALUE", invalid.getMessage()));
                return Mono.empty();
            }
            scope = authorizedApplicationScope(row, application, batch);
        }
        return scope.flatMap(resolved -> unique(roleMode, row, normalizedName, resolved, batch));
    }

    /** Aplicación del tenant del principal (si no existe ahí, es «no encontrada») y administrada por quien carga. */
    private Mono<RoleScope> authorizedApplicationScope(BulkCatalogWorkbookService.CatalogRow row, ApplicationName name, Batch batch) {
        TenantId tenant = batch.principal().tenantId();
        return Mono.defer(() -> applications.execute(new ApplicationNameForTenantQuery(tenant, name)))
                .switchIfEmpty(Mono.defer(() -> {
                    batch.errors().add(error(row.row(), APPLICATION_FIELD, "APPLICATION_NOT_FOUND", APPLICATION_NOT_FOUND_MESSAGE));
                    return Mono.empty();
                }))
                .onErrorResume(failure -> {
                    batch.errors().add(error(row.row(), APPLICATION_FIELD, "INVALID_VALUE", failure.getMessage()));
                    return Mono.empty();
                })
                .flatMap((RegisteredApplicationResponse application) -> isAdministrator(application.id(), batch).flatMap(allowed -> {
                    if (!allowed) {
                        batch.errors().add(error(row.row(), APPLICATION_FIELD, "NOT_AUTHORIZED", NOT_AUTHORIZED_MESSAGE));
                        return Mono.empty();
                    }
                    return Mono.just(RoleScope.ofApplication(tenant, application.id()));
                }));
    }

    /** Consulta el gate una sola vez por aplicación y por llamada; cualquier fallo del gate es una denegación. */
    private Mono<Boolean> isAdministrator(ApplicationId application, Batch batch) {
        Boolean known = batch.decisions().get(application);
        if (known != null) return Mono.just(known);
        PdpPrincipal principal = batch.principal();
        return Mono.defer(() -> administrators.execute(new AdministrationRequest(principal.tenantId(), application, batch.userId(), principal.subject(), Set.of(), principal.authenticationContext())))
                .thenReturn(true)
                .onErrorReturn(false)
                .doOnNext(decision -> batch.decisions().put(application, decision));
    }

    private Mono<Entry> unique(boolean roleMode, BulkCatalogWorkbookService.CatalogRow row, String name, RoleScope scope, Batch batch) {
        String nameField = roleMode ? ROLE_NAME_FIELD : PROFILE_NAME_FIELD;
        String key = name.toLowerCase(Locale.ROOT) + "|" + scope;
        if (!batch.seen().add(key)) {
            batch.errors().add(error(row.row(), "Nombre", "DUPLICATE_IN_FILE", DUPLICATE_NAME_MESSAGE));
            return Mono.empty();
        }
        Mono<Boolean> exists = Mono.defer(() -> roleMode ? roles.existsByNameInScope(new RoleName(name), scope) : profiles.existsByNameInScope(new ProfileName(name), scope));
        return exists.flatMap(found -> {
            if (found) {
                batch.errors().add(error(row.row(), nameField, "DUPLICATE_PERSISTED", roleMode ? ROLE_EXISTS_MESSAGE : PROFILE_EXISTS_MESSAGE));
                return Mono.<Entry>empty();
            }
            return Mono.just(new Entry(row, scope, name));
        }).onErrorResume(failure -> {
            batch.errors().add(error(row.row(), nameField, "INVALID_VALUE", failure.getMessage()));
            return Mono.empty();
        });
    }

    // ---- roles x recursos -------------------------------------------------------------------------------------

    private Mono<Void> resolveRoleResources(byte[] bytes, Batch batch) {
        BulkCatalogWorkbookService.Parsed<BulkCatalogWorkbookService.RoleResourceRow> parsed = books.parseRoleResources(bytes);
        batch.relationErrors().addAll(parsed.errors());
        Set<Integer> unreadable = rowsOf(parsed.errors());
        Set<String> seen = new HashSet<>();
        return Flux.fromIterable(parsed.rows()).concatMap(row -> {
            if (unreadable.contains(row.row())) return Mono.<Void>empty();
            List<String> missing = new ArrayList<>();
            if (row.role().isBlank()) missing.add(ROLE_NAME_REQUIRED_MESSAGE);
            if (row.scope().isBlank()) missing.add(SCOPE_REQUIRED_MESSAGE);
            if (row.application().isBlank()) missing.add(APPLICATION_REQUIRED_MESSAGE);
            if (row.path().isBlank()) missing.add(PATH_REQUIRED_MESSAGE);
            if (row.method().isBlank()) missing.add(METHOD_REQUIRED_MESSAGE);
            if (!missing.isEmpty()) {
                missing.forEach(message -> batch.relationErrors().add(error(row.row(), ROLE_RESOURCES_SHEET, "REQUIRED", message)));
                return Mono.<Void>empty();
            }
            String key = row.role() + "|" + row.scope() + "|" + row.application() + "|" + row.path() + "|" + row.method();
            if (!seen.add(key)) {
                batch.relationErrors().add(error(row.row(), ROLE_RESOURCES_SHEET, "DUPLICATE_IN_FILE", DUPLICATE_ROLE_RESOURCE_MESSAGE));
                return Mono.<Void>empty();
            }
            return roleResource(row, batch);
        }).then();
    }

    private Mono<Void> roleResource(BulkCatalogWorkbookService.RoleResourceRow row, Batch batch) {
        TenantId tenant = batch.principal().tenantId();
        List<BulkResourceRowError> errors = batch.relationErrors();
        try {
            ApplicationName applicationName = new ApplicationName(row.application());
            return Mono.defer(() -> applications.execute(new ApplicationNameForTenantQuery(tenant, applicationName)))
                    .switchIfEmpty(Mono.error(new IllegalArgumentException(APPLICATION_NOT_FOUND_MESSAGE)))
                    .flatMap(app -> {
                        RoleScope scope = RoleScopeLevel.parse(row.scope()) == RoleScopeLevel.TENANT ? RoleScope.ofTenant(tenant) : RoleScope.ofApplication(tenant, app.id());
                        Entry entry = batch.accepted().stream().filter(e -> e.name().equalsIgnoreCase(row.role()) && e.scope().equals(scope)).findFirst().orElse(null);
                        if (entry == null) {
                            errors.add(error(row.row(), ROLE_NAME_FIELD, "ROLE_NOT_FOUND", ROLE_NOT_IN_SHEET_MESSAGE));
                            return Mono.<Void>empty();
                        }
                        return resources.findIdByApplicationPathAndMethod(app.id(), new ResourcePath(row.path()), HttpVerb.parse(row.method()))
                                .switchIfEmpty(Mono.error(new IllegalArgumentException(RESOURCE_NOT_FOUND_MESSAGE)))
                                .doOnNext(entry.resources()::add)
                                .then();
                    })
                    .onErrorResume(failure -> {
                        errors.add(error(row.row(), ROLE_RESOURCES_SHEET, "INVALID_REFERENCE", failure.getMessage()));
                        return Mono.empty();
                    });
        } catch (RuntimeException invalid) {
            errors.add(error(row.row(), ROLE_RESOURCES_SHEET, "INVALID_VALUE", invalid.getMessage()));
            return Mono.empty();
        }
    }

    // ---- perfiles x roles -------------------------------------------------------------------------------------

    private Mono<Void> resolveProfileRoles(byte[] bytes, Batch batch) {
        BulkCatalogWorkbookService.Parsed<BulkCatalogWorkbookService.ProfileRoleRow> parsed = books.parseProfileRoles(bytes);
        batch.relationErrors().addAll(parsed.errors());
        Set<Integer> unreadable = rowsOf(parsed.errors());
        Set<String> seen = new HashSet<>();
        return Flux.fromIterable(parsed.rows()).concatMap(row -> {
            if (unreadable.contains(row.row())) return Mono.<Void>empty();
            List<String> missing = new ArrayList<>();
            if (row.profile().isBlank()) missing.add(PROFILE_NAME_REQUIRED_MESSAGE);
            if (row.profileScope().isBlank()) missing.add(PROFILE_SCOPE_REQUIRED_MESSAGE);
            if (row.role().isBlank()) missing.add(ROLE_NAME_REQUIRED_MESSAGE);
            if (row.roleScope().isBlank()) missing.add(ROLE_SCOPE_REQUIRED_MESSAGE);
            if (!missing.isEmpty()) {
                missing.forEach(message -> batch.relationErrors().add(error(row.row(), PROFILE_ROLES_SHEET, "REQUIRED", message)));
                return Mono.<Void>empty();
            }
            String key = row.profile() + "|" + row.profileScope() + "|" + row.profileApplication() + "|" + row.role() + "|" + row.roleScope() + "|" + row.roleApplication();
            if (!seen.add(key)) {
                batch.relationErrors().add(error(row.row(), PROFILE_ROLES_SHEET, "DUPLICATE_IN_FILE", DUPLICATE_PROFILE_ROLE_MESSAGE));
                return Mono.<Void>empty();
            }
            return profileRole(row, batch);
        }).then();
    }

    private Mono<Void> profileRole(BulkCatalogWorkbookService.ProfileRoleRow row, Batch batch) {
        TenantId tenant = batch.principal().tenantId();
        List<BulkResourceRowError> errors = batch.relationErrors();
        try {
            return relationScope(row.profileScope(), row.profileApplication(), tenant).flatMap(profileScope -> {
                Entry profile = batch.accepted().stream().filter(e -> e.name().equalsIgnoreCase(row.profile()) && e.scope().equals(profileScope)).findFirst().orElse(null);
                if (profile == null) {
                    errors.add(error(row.row(), PROFILE_ROLES_SHEET, "PROFILE_NOT_FOUND", PROFILE_NOT_IN_SHEET_MESSAGE));
                    return Mono.<Void>empty();
                }
                return relationScope(row.roleScope(), row.roleApplication(), tenant)
                        .flatMap(roleScope -> roles.findByNameInScope(new RoleName(row.role()), roleScope)
                                .switchIfEmpty(Mono.error(new IllegalArgumentException(REFERENCED_ROLE_NOT_FOUND_MESSAGE)))
                                .doOnNext(role -> profile.roles().add(role.id()))
                                .then());
            }).onErrorResume(failure -> {
                errors.add(error(row.row(), PROFILE_ROLES_SHEET, "INVALID_REFERENCE", failure.getMessage()));
                return Mono.empty();
            });
        } catch (RuntimeException invalid) {
            errors.add(error(row.row(), PROFILE_ROLES_SHEET, "INVALID_VALUE", invalid.getMessage()));
            return Mono.empty();
        }
    }

    /** Alcance de una referencia de relación: no exige administrar la aplicación (solo apunta a lo ya aceptado o persistido). */
    private Mono<RoleScope> relationScope(String scope, String application, TenantId tenant) {
        RoleScopeLevel level = RoleScopeLevel.parse(scope);
        if (level == RoleScopeLevel.TENANT) {
            if (!application.isBlank()) throw new IllegalArgumentException(APPLICATION_ONLY_FOR_APPLICATION_SCOPE_MESSAGE);
            return Mono.just(RoleScope.ofTenant(tenant));
        }
        if (level != RoleScopeLevel.APPLICATION || application.isBlank())
            throw new IllegalArgumentException(APPLICATION_SCOPE_REQUIRES_APPLICATION_MESSAGE);
        ApplicationName name = new ApplicationName(application);
        return Mono.defer(() -> applications.execute(new ApplicationNameForTenantQuery(tenant, name)))
                .map(found -> RoleScope.ofApplication(tenant, found.id()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException(APPLICATION_NOT_FOUND_MESSAGE)));
    }

    // ---- resultado --------------------------------------------------------------------------------------------

    private Result result(boolean roleMode, int total, Batch batch) {
        BulkResourceValidationResponse response = response(total, batch.errors(), batch.relationErrors());
        if (roleMode) {
            List<Role> built = batch.accepted().stream().map(e -> new Role(new RoleId(ids.next()), new RoleName(e.name()), e.scope(), e.resources(), time.now())).toList();
            return new Result(response, built, List.of());
        }
        List<Profile> built = batch.accepted().stream().map(e -> new Profile(new ProfileId(ids.next()), new ProfileName(e.name()), e.scope(), e.roles(), time.now())).toList();
        return new Result(response, List.of(), built);
    }

    /**
     * {@code invalidRows} son las filas distintas de la hoja principal con algún error; los errores de las hojas de
     * relación se listan y bloquean la carga, pero no son filas de roles o perfiles y no entran en los contadores.
     */
    private BulkResourceValidationResponse response(int total, List<BulkResourceRowError> sheetErrors, List<BulkResourceRowError> relationErrors) {
        int invalid = (int) Math.min(total, sheetErrors.stream().map(BulkResourceRowError::row).filter(i -> i > 0).distinct().count());
        List<BulkResourceRowError> all = new ArrayList<>(sheetErrors);
        all.addAll(relationErrors);
        return new BulkResourceValidationResponse(all.isEmpty(), total, total - invalid, invalid, List.copyOf(all));
    }

    private static Set<Integer> rowsOf(List<BulkResourceRowError> errors) {
        return errors.stream().map(BulkResourceRowError::row).filter(i -> i > 0).collect(Collectors.toSet());
    }

    private static BulkResourceRowError error(int row, String field, String code, String description) {
        return new BulkResourceRowError(row, field, "", code, description == null ? code : description);
    }

    /** Estado de una sola llamada a validar o confirmar: nada se comparte entre llamadas. */
    private record Batch(PdpPrincipal principal, UserId userId, List<BulkResourceRowError> errors,
                         List<BulkResourceRowError> relationErrors, List<Entry> accepted, Set<String> seen,
                         Map<ApplicationId, Boolean> decisions, Set<Integer> cellErrorRows) {
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
