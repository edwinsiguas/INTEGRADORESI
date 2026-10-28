package pe.marcona.mtc.service;

import java.util.Map;

/**
 * Contrato de servicio de solo lectura para la consulta y búsqueda de vehículos (CQRS - Query).
 */
public interface IVehiculoConsultaService {

    /**
     * Lista los vehículos registrados según los criterios de filtrado y paginación.
     */
    Map<String, Object> listar(Map<String, String> consulta);
}
