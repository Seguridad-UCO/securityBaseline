@org.springframework.modulith.ApplicationModule(allowedDependencies = {
        "commons",
        "tenants", "tenants :: dto", "tenants :: rule",
        "applications", "applications :: repository", "applications :: exception"})
package co.edu.uco.seguridad.pdp.resources;
