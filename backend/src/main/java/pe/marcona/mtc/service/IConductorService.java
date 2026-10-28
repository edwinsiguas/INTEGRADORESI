package pe.marcona.mtc.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Contrato de servicio para la gestión exclusiva de Conductores/Choferes.
 */
public interface IConductorService {

    Map<String, Object> listarConductores(Map<String, String> consulta);

    Map<String, Object> crearConductor(Map<String, Object> entrada, HttpServletRequest solicitud, long idActor);

    Map<String, Object> actualizarConductor(Map<String, Object> entrada, HttpServletRequest solicitud, long idActor);
}
