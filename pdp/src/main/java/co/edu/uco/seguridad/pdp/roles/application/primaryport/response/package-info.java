/**
 * DTO de salida del núcleo de {@code roles} que otros módulos consumen (HU-015: {@code assignments}
 * necesita {@code RoleResponse} tras {@code DefineRoleUseCase}). Mismo nombre de NamedInterface
 * ("dto") que {@code roles/application/primaryport/request} — ambos subpaquetes forman una sola
 * interfaz nombrada lógica, igual que en {@code applications :: dto}.
 */
@org.springframework.modulith.NamedInterface("dto")
package co.edu.uco.seguridad.pdp.roles.application.primaryport.response;
