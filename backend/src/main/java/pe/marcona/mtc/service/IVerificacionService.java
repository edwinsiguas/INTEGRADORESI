package pe.marcona.mtc.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Contrato de servicio para el registro y consulta de verificaciones de vehículos y conductores.
 */
public interface IVerificacionService {

    Map<String, Object> crear(Map<String, Object> entrada, HttpServletRequest solicitud, Map<String, Object> usuario);

    Map<String, Object> listar(Map<String, String> consulta, Map<String, Object> usuario);
}
