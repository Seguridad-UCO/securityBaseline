package co.edu.uco.seguridad.pdp.authorization.application.primaryport.response;
public record ApplicationSecuritySummaryResponse(Application application, Counts counts) { public record Application(String id,String name,String description,String baseUrl){} public record Counts(long resources,long roles,long profiles,long administrators,long roleAssignments,long profileAssignments){} }
