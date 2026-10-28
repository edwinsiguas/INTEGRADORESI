package pe.marcona.mtc.service;

import java.util.Map;

/**
 * Contrato de servicio para validar las condiciones de circulación de vehículos y conductores.
 */
public interface ICirculacionService {

    Map<String, Object> validar(Map<String, Object> entrada);
}

