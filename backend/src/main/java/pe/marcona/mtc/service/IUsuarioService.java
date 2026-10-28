package pe.marcona.mtc.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Contrato de servicio para la gestión de usuarios del sistema.
 */
public interface IUsuarioService {

    /**
     * Lista los usuarios del sistema aplicando filtros de búsqueda, rol y estado con paginación.
     */
    Map<String, Object> listar(Map<String, String> consulta, HttpServletRequest solicitud);

    /**
     * Registra un nuevo usuario en el sistema con credenciales cifradas y genera auditoría.
     */
    Map<String, Object> crear(Map<String, Object> datosEntrada, HttpServletRequest solicitud);

    /**
     * Actualiza la información y/o credenciales de un usuario existente.
     */
    Map<String, Object> actualizar(Map<String, Object> datosEntrada, HttpServletRequest solicitud);
}

