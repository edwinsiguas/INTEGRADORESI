package pe.marcona.mtc.dao.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.marcona.mtc.dao.IAutenticacionDao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación de acceso a datos para autenticación con consultas parametrizadas.
 */
@Repository
public class AutenticacionDaoImpl implements IAutenticacionDao {

    private final JdbcTemplate jdbcTemplate;

    public AutenticacionDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Map<String, Object>> obtenerUsuarioActivoPorEmail(String email) {
        String sql = """
                SELECT id, email, password, nombre, role, estado, is_active
                FROM users
                WHERE email = ? AND estado = ? AND is_active = 1
                """;
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(sql, email, "activo");
        return filas.isEmpty() ? Optional.empty() : Optional.of(filas.get(0));
    }

    @Override
    public Optional<Map<String, Object>> obtenerUsuarioPorId(long id) {
        String sql = """
                SELECT id, email, nombre, role, estado, is_active
                FROM users
                WHERE id = ?
                """;
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(sql, id);
        return filas.isEmpty() ? Optional.empty() : Optional.of(filas.get(0));
    }
}
