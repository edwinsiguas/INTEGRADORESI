package pe.marcona.mtc.dao;

import java.util.List;
import java.util.Map;

/**
 * Contrato de acceso a datos para las fiscalizaciones y verificaciones vehiculares en vía pública.
 */
public interface IVerificacionDao {

    long insertar(
            long idUsuario,
            String tipoBusqueda,
            String consultaBusqueda,
            String resultado,
            String motivosJson,
            String datosVehiculoJson,
            String direccionIp);

    long contarConFiltros(String condicionWhere, List<Object> parametros);

    List<Map<String, Object>> listarConFiltros(String condicionWhere, List<Object> parametros, int limite, long offset);

    Map<String, Object> obtenerEstadisticasHoy(long idUsuario);
}
