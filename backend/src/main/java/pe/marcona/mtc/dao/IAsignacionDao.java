package pe.marcona.mtc.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad de Asignaciones Vehículo-Conductor.
 */
public interface IAsignacionDao {

    long contarAsignaciones(Long idVehiculo, Long idConductor, String tipo, Integer activo);

    List<Map<String, Object>> listarAsignaciones(Long idVehiculo, Long idConductor, String tipo,
                                                Integer activo, int limite, long desplazamiento);

    Optional<Map<String, Object>> obtenerVehiculoPorId(long idVehiculo);

    Optional<Map<String, Object>> obtenerConductorPorId(long idConductor);

    int desactivarAsignacionActivaPorVehiculo(long idVehiculo);

    long insertarAsignacion(long idVehiculo, long idConductor, String tipo, String propietarioNombre,
                            String propietarioDni, String fechaFin, String notas, long idCreador);

    Optional<Map<String, Object>> obtenerPorId(long id);

    Optional<Map<String, Object>> obtenerActivaPorVehiculo(long idVehiculo);

    Optional<Map<String, Object>> obtenerActivaPorConductor(long idConductor);

    List<Map<String, Object>> obtenerHistorialPorVehiculo(long idVehiculo);

    int desasignarVehiculo(long idVehiculo);
}

