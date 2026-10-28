package pe.marcona.mtc.dao;

import java.util.List;
import java.util.Map;

/**
 * Contrato de acceso a datos para la extracción y exportación masiva de registros del sistema.
 */
public interface IExportacionDao {

    List<Map<String, Object>> consultarEmpresasParaExportar(String condicionWhere, List<Object> parametros);

    List<Map<String, Object>> consultarConductoresParaExportar(String condicionWhere, List<Object> parametros);

    List<Map<String, Object>> consultarVehiculosParaExportar(String condicionWhere, List<Object> parametros);

    List<Map<String, Object>> consultarPermisosParaExportar(String condicionWhere, List<Object> parametros);
}

