package pe.marcona.mtc.dao;

import java.util.Map;
import java.util.Optional;

/**
 * Contrato de acceso a datos para autenticación y consulta de credenciales de usuario.
 */
public interface IAutenticacionDao {

    Optional<Map<String, Object>> obtenerUsuarioActivoPorEmail(String email);

    Optional<Map<String, Object>> obtenerUsuarioPorId(long id);
}
