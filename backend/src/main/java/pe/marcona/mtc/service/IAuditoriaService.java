package pe.marcona.mtc.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Contrato de servicio para el registro inmutable y la consulta de auditoría de acciones del sistema.
 */
public interface IAuditoriaService {

    /**
     * Registra un evento de auditoría en la base de datos y en archivo JSONL para trazabilidad forense.
     */
    void registrarAccion(long idActor, String accion, String entidad, long idEntidad, String descripcion, HttpServletRequest solicitud);

    /**
     * Consulta el historial paginado de acciones del sistema aplicando filtros.
     */
    Map<String, Object> listarHistorial(Map<String, String> consulta);
}
