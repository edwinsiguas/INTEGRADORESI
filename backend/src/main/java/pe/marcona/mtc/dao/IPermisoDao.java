package pe.marcona.mtc.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad Permisos y consultas operativas asociadas.
 */
public interface IPermisoDao {

    long contarPermisos(String busqueda, String estado, Long idVehiculo, Long idConductor, Long idEmpresa, boolean proximoAVencer);

    List<Map<String, Object>> listarPermisos(String busqueda, String estado, Long idVehiculo, Long idConductor,
                                            Long idEmpresa, boolean proximoAVencer, int limite, long desplazamiento);

    Optional<Map<String, Object>> obtenerPorId(long id);

    Optional<Map<String, Object>> obtenerVehiculoPorId(long idVehiculo);

    Optional<Map<String, Object>> obtenerAsignacionActivaPorVehiculo(long idVehiculo);

    int contarPermisosVigentesPorAsignacion(long idAsignacion);

    Optional<Map<String, Object>> obtenerConductorPorId(long idConductor);

    int contarAsignacionesActivasPorId(long idAsignacion);

    long insertarPermiso(Long idAsignacion, long idVehiculo, long idConductor, Long idEmpresa,
                         String numeroPermiso, String tipoPermiso, LocalDate fechaVencimiento,
                         String observaciones, String qrCode, long idCreador,
                         String numeroRecibo, BigDecimal montoPagado);

    boolean revocarPermiso(long idPermiso, String motivo);

    int actualizarEstado(long idPermiso, String nuevoEstado);

    List<Map<String, Object>> obtenerImagenesVehiculo(long idVehiculo);

    List<Long> obtenerIdsPermisosVencidosParaBloqueo();

    int marcarComoVencido(long idPermiso);

    int obtenerSiguienteSecuencia(int anio);
}

