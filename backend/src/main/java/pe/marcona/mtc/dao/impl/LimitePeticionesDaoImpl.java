package pe.marcona.mtc.dao.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.marcona.mtc.dao.ILimitePeticionesDao;

/**
 * Implementación de persistencia para límites de peticiones utilizando consultas parametrizadas.
 */
@Repository
public class LimitePeticionesDaoImpl implements ILimitePeticionesDao {

    private final JdbcTemplate jdbcTemplate;

    public LimitePeticionesDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int contarPeticionesEnVentana(String ip, String accion, int segundosVentana) {
        String sql = """
                SELECT COUNT(*)
                FROM rate_limits
                WHERE ip = ? AND action = ?
                  AND created_at > DATE_SUB(NOW(), INTERVAL ? SECOND)
                """;
        Integer conteo = jdbcTemplate.queryForObject(sql, Integer.class, ip, accion, segundosVentana);
        return conteo == null ? 0 : conteo;
    }

    @Override
    public Long obtenerSegundosRestantes(String ip, String accion, int segundosVentana) {
        String sql = """
                SELECT TIMESTAMPDIFF(SECOND, NOW(), DATE_ADD(MIN(created_at), INTERVAL ? SECOND))
                FROM rate_limits
                WHERE ip = ? AND action = ?
                  AND created_at > DATE_SUB(NOW(), INTERVAL ? SECOND)
                """;
        return jdbcTemplate.queryForObject(sql, Long.class, segundosVentana, ip, accion, segundosVentana);
    }

    @Override
    public void registrarPeticion(String ip, String accion) {
        String sql = "INSERT INTO rate_limits (ip, action) VALUES (?, ?)";
        jdbcTemplate.update(sql, ip, accion);
    }
}
