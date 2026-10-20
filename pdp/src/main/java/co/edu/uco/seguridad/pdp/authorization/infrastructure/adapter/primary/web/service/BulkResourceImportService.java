package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationNameForTenantQuery;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationNameForTenantLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.model.*;
import co.edu.uco.seguridad.pdp.resources.domain.event.ProtectedResourceRegistered;
import co.edu.uco.seguridad.shared.event.DomainEventPublisher;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.*;

/** Valida contra el tenant y el gate administrativo antes de construir un lote transaccional. */
public final class BulkResourceImportService {
 private final BulkResourceWorkbookService workbooks; private final ApplicationNameForTenantLookupValidator applications;
 private final PrincipalMustBeApplicationAdministratorValidator administrators; private final ProtectedResourceRepository resources;
 private final IdentifierGenerator identifiers; private final TimeProvider time; private final DomainEventPublisher events;
 public BulkResourceImportService(BulkResourceWorkbookService w, ApplicationNameForTenantLookupValidator a, PrincipalMustBeApplicationAdministratorValidator ad, ProtectedResourceRepository r, IdentifierGenerator i, TimeProvider t, DomainEventPublisher events){workbooks=w;applications=a;administrators=ad;resources=r;identifiers=i;time=t;this.events=events;}
 public Mono<Result> validate(byte[] bytes, PdpPrincipal principal, UserId userId) {
  BulkResourceWorkbookService.ParsedWorkbook parsed=workbooks.parse(bytes); List<BulkResourceValidationResponse.BulkResourceRowError> errors=new ArrayList<>(parsed.errors()); List<Resolved> valid=new ArrayList<>();
  return Flux.fromIterable(parsed.rows()).concatMap(row -> resolve(row,principal,userId,errors).doOnNext(valid::add)).then(Mono.fromSupplier(()->result(parsed.rows().size(),errors,valid)));
 }
 public Mono<Result> commit(byte[] bytes, PdpPrincipal principal, UserId userId) { return validate(bytes,principal,userId).flatMap(result -> result.response().ready() ? resources.saveAllAtomically(result.resources()).then(Flux.fromIterable(result.resources()).concatMap(resource -> events.publish(ProtectedResourceRegistered.of(resource))).then()).thenReturn(result) : Mono.just(result)); }
 private Mono<Resolved> resolve(BulkResourceWorkbookService.RowValue row,PdpPrincipal principal,UserId userId,List<BulkResourceValidationResponse.BulkResourceRowError> errors) {
  final ApplicationName name; final ResourcePath path; final HttpVerb method;
  try { name=new ApplicationName(row.applicationName()); path=new ResourcePath(row.path()); method=HttpVerb.parse(row.method()); }
  catch (RuntimeException error) { errors.add(error(row.row(),"row","","INVALID_VALUE",error.getMessage())); return Mono.empty(); }
  return applications.execute(new ApplicationNameForTenantQuery(principal.tenantId(),name))
   .switchIfEmpty(Mono.defer(()->{errors.add(error(row.row(),"Nombre de aplicación",name.value(),"APPLICATION_NOT_FOUND","La aplicación no pertenece al tenant autenticado"));return Mono.empty();}))
   .flatMap(application -> administrators.execute(new AdministrationRequest(principal.tenantId(),application.id(),userId,principal.subject(),Set.of(),principal.authenticationContext()))
       .then(resources.existsByApplicationPathAndMethod(application.id(),path,method))
       .flatMap(exists -> {if(exists){errors.add(error(row.row(),"row","","DUPLICATE_PERSISTED","El recurso ya existe"));return Mono.empty();} return Mono.just(new Resolved(application.id(),path,method, principal.tenantId()));}))
   .onErrorResume(error -> { errors.add(error(row.row(),"row","","NOT_AUTHORIZED", "No está autorizado para administrar esta aplicación")); return Mono.empty(); });
 }
 private Result result(int total,List<BulkResourceValidationResponse.BulkResourceRowError> errors,List<Resolved> valid){ long invalid=errors.stream().map(BulkResourceValidationResponse.BulkResourceRowError::row).filter(n->n>0).distinct().count(); List<ProtectedResource> entities=valid.stream().map(v->ProtectedResource.register(new ResourceId(identifiers.next()),v.applicationId(),v.tenantId(),v.path(),v.method(),time.now())).toList(); return new Result(new BulkResourceValidationResponse(errors.isEmpty(),total,total-(int)Math.min(total,invalid),(int)Math.min(total,invalid),List.copyOf(errors)),entities); }
 private static BulkResourceValidationResponse.BulkResourceRowError error(int row,String field,String value,String code,String description){return new BulkResourceValidationResponse.BulkResourceRowError(row,field,value,code,description==null?code:description);}
 private record Resolved(ApplicationId applicationId, ResourcePath path, HttpVerb method, TenantId tenantId) { }
 public record Result(BulkResourceValidationResponse response,List<ProtectedResource> resources) { }
}
