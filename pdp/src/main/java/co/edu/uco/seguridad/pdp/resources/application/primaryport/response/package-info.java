/**
 * DTO de salida del núcleo de {@code resources} que otros módulos consumen (HU-017: {@code authorization}
 * necesita {@code RegisteredProtectedResourceResponse} tras {@code RegisterProtectedResourceUseCase}).
 * Mismo nombre de NamedInterface ("dto") que {@code resources/application/primaryport/request} —
 * ambos subpaquetes forman una sola interfaz nombrada lógica, igual que en {@code roles :: dto}.
 */
@org.springframework.modulith.NamedInterface("dto")
package co.edu.uco.seguridad.pdp.resources.application.primaryport.response;
