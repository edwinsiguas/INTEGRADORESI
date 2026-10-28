package pe.marcona.mtc.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Contrato de servicio para la gestión exclusiva de Empresas de transporte.
 */
public interface IEmpresaService {

    Map<String, Object> listarEmpresas(Map<String, String> consulta);

    Map<String, Object> obtenerEmpresa(long id);

    Map<String, Object> crearEmpresa(Map<String, Object> entrada, HttpServletRequest solicitud, long idActor);

    Map<String, Object> actualizarEmpresa(Map<String, Object> entrada, HttpServletRequest solicitud, long idActor);

    Map<String, Object> alternarEstado(long id, HttpServletRequest solicitud, long idActor);
}

