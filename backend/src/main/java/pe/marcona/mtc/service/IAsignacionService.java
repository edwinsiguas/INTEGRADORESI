package pe.marcona.mtc.service;

import java.util.List;
import java.util.Map;

/**
 * Contrato de servicio para la gestión de asignaciones de conductores a vehículos.
 */
public interface IAsignacionService {

    Map<String, Object> listar(Map<String, String> consulta);

    Map<String, Object> asignar(Map<String, Object> datosEntrada, long idActor);

    Map<String, Object> activaPorVehiculo(long idVehiculo);

    Map<String, Object> activaPorConductor(long idConductor);

    List<Map<String, Object>> historial(long idVehiculo);

    boolean desasignar(long idVehiculo);
}
