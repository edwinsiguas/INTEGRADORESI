package pe.marcona.mtc.dao;

import java.util.List;
import java.util.Map;

/**
 * Contrato de acceso a datos de solo lectura para la entidad Vehículos (CQRS - Query).
 */
public interface IVehiculoConsultaDao {

    long contarVehiculos(String busqueda, Long idEmpresa, Long idConductor, String estado, boolean incluirInactivos);

    List<Map<String, Object>> listarVehiculos(String busqueda, Long idEmpresa, Long idConductor,
                                             String estado, boolean incluirInactivos, int limite, long desplazamiento);
}

