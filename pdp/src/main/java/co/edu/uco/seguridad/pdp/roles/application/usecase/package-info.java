/**
 * Primer {@code @NamedInterface} de este subpaquete (HU-015): {@code assignments} necesita
 * {@code DefineRoleUseCase} para crear el rol {@code ADMIN} de una aplicación al orquestar su
 * primer administrador. {@code roles} ya tiene otros NamedInterfaces con consumidores (rule, dto,
 * model, exception), así que esto no dispara la trampa de "primer NamedInterface del módulo" — solo
 * abre este subpaquete concreto.
 */
@org.springframework.modulith.NamedInterface("usecase")
package co.edu.uco.seguridad.pdp.roles.application.usecase;
