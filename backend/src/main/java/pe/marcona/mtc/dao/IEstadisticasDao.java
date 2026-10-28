package pe.marcona.mtc.dao;

import java.util.List;
import java.util.Map;

/**
 * Contrato de acceso a datos para métricas, agregaciones e indicadores estadísticos del sistema.
 */
public interface IEstadisticasDao {

    long contarVehiculos();

    long contarVehiculosPorEstado(String estado);

    long contarEmpresas();

    long contarEmpresasPorEstado(String estado);

    long contarConductores();

    long contarConductoresPorEstado(String estado);

    long contarPermisosVigentes();

    long contarPermisosPorVencer(int dias);

    long contarVehiculosSoatVigente();

    long contarVehiculosRtVigente();

    long contarConductoresLicenciaVigente();

    long contarTramitesMesActual();

    List<Map<String, Object>> obtenerTramitesPorMesUltimoSemestre();

    List<Map<String, Object>> obtenerCrecimientoVehicularPorMes();

    List<Map<String, Object>> obtenerMesesSecuencia();

    List<Map<String, Object>> obtenerDistribucionTipoServicio();

    List<Map<String, Object>> obtenerRankingEmpresasFlota();
}

