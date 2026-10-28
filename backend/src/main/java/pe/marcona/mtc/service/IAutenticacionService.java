package pe.marcona.mtc.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

/**
 * Contrato de servicio para el control de sesiones, autenticación y autorización basada en roles.
 */
public interface IAutenticacionService {

    Map<String, Object> iniciarSesion(Map<String, Object> entrada, HttpServletRequest solicitud);

    Map<String, Object> cerrarSesion(HttpServletRequest solicitud);

    Map<String, Object> verificarToken(HttpServletRequest solicitud);

    Map<String, Object> requerirUsuario(HttpServletRequest solicitud, String rolRequerido);

    Map<String, Object> requerirUsuarioEnRoles(HttpServletRequest solicitud, Set<String> rolesRequeridos);
}

