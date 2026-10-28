package pe.marcona.mtc.dao;

import java.util.List;
import java.util.Map;

/**
 * Contrato de acceso a datos para registros de auditoría y trazas de acciones (action_logs).
 */
public interface IAuditoriaDao {

    void insertarRegistro(long idUsuario, String accion, String entidad, long idEntidad, String descripcion, String ip);

    long contarHistorial(Long idUsuario, String accion, String entidad);

    List<Map<String, Object>> listarHistorial(Long idUsuario, String accion, String entidad, int limite, long desplazamiento);
}
