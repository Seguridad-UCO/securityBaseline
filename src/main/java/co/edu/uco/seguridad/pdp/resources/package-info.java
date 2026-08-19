@org.springframework.modulith.ApplicationModule(allowedDependencies = {
        "commons",
        "tenants", "tenants :: dto", "tenants :: rule",
        "applications :: usecase", "applications :: dto", "applications :: exception"})
package co.edu.uco.seguridad.pdp.resources;
