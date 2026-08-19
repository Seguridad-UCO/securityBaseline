package co.edu.uco.seguridad.pdp.platform.infrastructure;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.platform.application.PlatformAdministrationService;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Adaptador SurrealDB del módulo de administración. La validación de formatos vive en este borde de aplicación. */
public final class PlatformAdministrationServiceImpl implements PlatformAdministrationService {
    private static final String DEFAULT_TENANT = "universidad-uco";
    private final SurrealDbClient db;
    public PlatformAdministrationServiceImpl(SurrealDbClient db) { this.db = db; }

    @Override public Mono<LocalUserPrincipal> provision(String issuer, String subject, String email, String name) {
        String now = Instant.now().toString();
        return db.execute("SELECT * FROM external_identity WHERE issuer=$issuer AND subject=$subject LIMIT 1;", Map.of("issuer", issuer, "subject", subject))
                .flatMap(result -> result.getFirst().isEmpty() ? createOrLink(issuer, subject, email, name, now) : loadKnown(result.getFirst().get(0), now, name));
    }
    private Mono<LocalUserPrincipal> loadKnown(JsonNode identity, String now, String name) {
        String userId = identity.path("userId").asString();
        return db.execute("UPDATE type::record('security_user',$id) SET lastLoginAt=<datetime>$now, name=$name; SELECT * FROM type::record('security_user',$id);", Map.of("id", userId,"now",now,"name",name))
                .map(r -> principal(userId, r.get(1).get(0), identity.path("subject").asString()));
    }
    private Mono<LocalUserPrincipal> createOrLink(String issuer, String subject, String email, String name, String now) {
        return db.execute("SELECT * FROM security_user WHERE email=$email LIMIT 1;", Map.of("email", email.toLowerCase(Locale.ROOT)))
                .flatMap(r -> {
                    String id = r.getFirst().isEmpty() ? UUID.randomUUID().toString() : id(r.getFirst().get(0));
                    Mono<List<JsonNode>> saveUser = r.getFirst().isEmpty()
                            ? db.execute("CREATE type::record('security_user',$id) SET email=$email,name=$name,tenantId=$tenant,createdAt=<datetime>$now,lastLoginAt=<datetime>$now;", Map.of("id",id,"email",email.toLowerCase(Locale.ROOT),"name",name,"tenant",DEFAULT_TENANT,"now",now))
                            : db.execute("UPDATE type::record('security_user',$id) SET lastLoginAt=<datetime>$now,name=$name;", Map.of("id",id,"name",name,"now",now));
                    return saveUser.then(db.execute("CREATE type::record('external_identity',$id) SET issuer=$issuer,subject=$subject,userId=$userId,provider=$provider;", Map.of("id",UUID.randomUUID().toString(),"issuer",issuer,"subject",subject,"userId",id,"provider",provider(issuer))))
                            .then(db.execute("SELECT * FROM type::record('security_user',$id);", Map.of("id",id))).map(rows -> principal(id, rows.getFirst().get(0), subject));
                });
    }
    private static String provider(String issuer) { return issuer.contains("google") ? "google" : "keycloak"; }
    /** Convierte un record id de SurrealDB (p. ej. {@code application:`uuid`}) al UUID público. */
    private static String id(JsonNode node) {
        String raw = node.path("id").asString();
        int separator = raw.indexOf(':');
        String value = separator < 0 ? raw : raw.substring(separator + 1);
        return value.length() >= 2 && value.startsWith("`") && value.endsWith("`")
                ? value.substring(1, value.length() - 1) : value;
    }
    private static LocalUserPrincipal principal(String id, JsonNode user, String subject) { return new LocalUserPrincipal(id, subject, new TenantId(user.path("tenantId").asString()), user.path("email").asString(), user.path("name").asString()); }
    @Override public Mono<List<ApplicationView>> applications(String tenant) { return db.execute("SELECT * FROM application WHERE tenantId=$tenant ORDER BY registeredAt DESC;",Map.of("tenant",tenant)).map(r->r.getFirst().valueStream().map(this::application).toList()); }
    @Override public Mono<ApplicationView> createApplication(String tenant,String name,String description,String baseUrl) { validName(name); validUrl(baseUrl); return db.execute("SELECT id FROM application WHERE tenantId=$tenant AND name=$name LIMIT 1;",Map.of("tenant",tenant,"name",name.trim())).flatMap(r->{if(!r.getFirst().isEmpty()) return Mono.error(new IllegalArgumentException("Ya existe una aplicación con ese nombre.")); String id=UUID.randomUUID().toString(),now=Instant.now().toString(); return db.execute("CREATE type::record('application',$id) SET tenantId=$tenant,name=$name,description=$description,baseUrl=$baseUrl,status='ACTIVE',registeredAt=<datetime>$now; SELECT * FROM type::record('application',$id);",Map.of("id",id,"tenant",tenant,"name",name.trim(),"description",description.trim(),"baseUrl",baseUrl.trim(),"now",now)).map(rows->application(rows.get(1).get(0)));}); }
    @Override public Mono<List<ResourceView>> resources(String tenant,String app) { return db.execute("SELECT * FROM protected_resource WHERE tenantId=$tenant AND applicationId=$app ORDER BY registeredAt DESC;",Map.of("tenant",tenant,"app",app)).map(r->r.getFirst().valueStream().map(this::resource).toList()); }
    @Override public Mono<ResourceView> createResource(String tenant,String app,String path,String method) { validPath(path); String verb=method.toUpperCase(Locale.ROOT); if(!List.of("GET","POST","PUT","PATCH","DELETE","HEAD","OPTIONS").contains(verb)) return Mono.error(new IllegalArgumentException("Método HTTP no permitido.")); return db.execute("SELECT id FROM application WHERE id=type::record('application',$app) AND tenantId=$tenant LIMIT 1;",Map.of("app",app,"tenant",tenant)).flatMap(a->{if(a.getFirst().isEmpty())return Mono.error(new IllegalArgumentException("La aplicación no existe en tu tenant."));return db.execute("SELECT id FROM protected_resource WHERE applicationId=$app AND path=$path AND method=$method LIMIT 1;",Map.of("app",app,"path",path,"method",verb)).flatMap(r->{if(!r.getFirst().isEmpty())return Mono.error(new IllegalArgumentException("Ese endpoint ya está registrado."));String id=UUID.randomUUID().toString(),now=Instant.now().toString();return db.execute("CREATE type::record('protected_resource',$id) SET applicationId=$app,tenantId=$tenant,path=$path,method=$method,registeredAt=<datetime>$now; SELECT * FROM type::record('protected_resource',$id);",Map.of("id",id,"app",app,"tenant",tenant,"path",path,"method",verb,"now",now)).map(rows->resource(rows.get(1).get(0)));});}); }
    @Override public Mono<List<TenantView>> tenants(){return db.execute("SELECT * FROM tenant ORDER BY name ASC;",Map.of()).map(r->r.getFirst().valueStream().map(this::tenant).toList());}
    @Override public Mono<TenantView> createTenant(String code,String name){if(!code.matches("[a-z][a-z0-9-]{1,62}"))return Mono.error(new IllegalArgumentException("El código debe usar minúsculas, números y guiones."));if(name==null||name.trim().length()<3)return Mono.error(new IllegalArgumentException("El nombre del tenant debe tener al menos 3 caracteres."));return db.execute("SELECT id FROM tenant WHERE id=type::record('tenant',$id) LIMIT 1;",Map.of("id",code)).flatMap(r->{if(!r.getFirst().isEmpty())return Mono.error(new IllegalArgumentException("Ese código de tenant ya existe."));return db.execute("CREATE type::record('tenant',$id) SET name=$name,status='ACTIVE'; SELECT * FROM type::record('tenant',$id);",Map.of("id",code,"name",name.trim())).map(rows->tenant(rows.get(1).get(0)));});}
    @Override public Mono<List<UserView>> users(){return db.execute("SELECT * FROM security_user ORDER BY lastLoginAt DESC;",Map.of()).map(r->r.getFirst().valueStream().map(this::user).toList());}
    @Override public Mono<UserView> assignTenant(String userId,String tenant){return db.execute("SELECT * FROM tenant WHERE id=type::record('tenant',$id) AND status='ACTIVE' LIMIT 1;",Map.of("id",tenant)).flatMap(t->{if(t.getFirst().isEmpty())return Mono.error(new IllegalArgumentException("El tenant no existe o está inactivo."));return db.execute("UPDATE type::record('security_user',$id) SET tenantId=$tenant; SELECT * FROM type::record('security_user',$id);",Map.of("id",userId,"tenant",tenant)).map(rows->user(rows.get(1).get(0)));});}
    private ApplicationView application(JsonNode x){return new ApplicationView(id(x),x.path("name").asString(),x.path("description").asString(),x.path("baseUrl").asString(),x.path("tenantId").asString(),x.path("registeredAt").asString());}
    private ResourceView resource(JsonNode x){return new ResourceView(id(x),x.path("applicationId").asString(),x.path("path").asString(),x.path("method").asString(),x.path("registeredAt").asString());}
    private TenantView tenant(JsonNode x){return new TenantView(id(x),x.path("name").asString(),x.path("status").asString());}
    private UserView user(JsonNode x){return new UserView(id(x),x.path("email").asString(),x.path("name").asString(),"federado",x.path("tenantId").asString(),x.path("createdAt").asString(),x.path("lastLoginAt").asString());}
    private static void validName(String value){if(value==null||value.trim().length()<3)throw new IllegalArgumentException("El nombre debe tener al menos 3 caracteres.");}
    private static void validUrl(String value){try{URI uri=URI.create(value);if(uri.getScheme()==null||uri.getHost()==null)throw new IllegalArgumentException();}catch(Exception e){throw new IllegalArgumentException("La URL base debe ser absoluta.");}}
    private static void validPath(String path){if(path==null||!path.matches("/(?:[A-Za-z0-9._~{}-]+/?)*"))throw new IllegalArgumentException("El path debe iniciar con / y no incluir query ni fragment.");}
}
