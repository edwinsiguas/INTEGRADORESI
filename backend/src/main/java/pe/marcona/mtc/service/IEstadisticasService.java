package pe.marcona.mtc.service;

import java.util.Map;

/**
 * Contrato de servicio para la obtención de métricas operativas, analíticas y paneles de control.
 */
public interface IEstadisticasService {

    Map<String, Object> obtenerDashboard();

    Map<String, Object> obtenerEmpresas();

    Map<String, Object> obtenerAnalitica();
}

