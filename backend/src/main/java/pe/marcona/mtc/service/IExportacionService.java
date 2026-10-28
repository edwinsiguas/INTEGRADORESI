package pe.marcona.mtc.service;

import java.util.List;
import java.util.Map;

/**
 * Contrato de servicio para la preparación y exportación de datos de entidades del sistema.
 */
public interface IExportacionService {

    List<Map<String, Object>> exportarEmpresas(Map<String, String> consulta);

    List<Map<String, Object>> exportarConductores(Map<String, String> consulta);

    List<Map<String, Object>> exportarVehiculos(Map<String, String> consulta);

    List<Map<String, Object>> exportarPermisos(Map<String, String> consulta);
}
