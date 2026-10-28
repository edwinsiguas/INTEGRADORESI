package pe.marcona.mtc.dao;

import java.util.Map;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la verificación de habilitación de circulación vehicular y de conductores.
 */
public interface ICirculacionDao {

    Optional<Map<String, Object>> buscarPorPlaca(String placa);

    Optional<Map<String, Object>> buscarPorDni(String dni);

    Optional<Map<String, Object>> buscarPorNumeroPermiso(String numeroPermiso);
}

