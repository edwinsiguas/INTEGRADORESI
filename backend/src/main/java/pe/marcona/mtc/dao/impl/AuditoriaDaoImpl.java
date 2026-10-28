package pe.marcona.mtc.dao.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.marcona.mtc.dao.IAuditoriaDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementación de persistencia para auditoría utilizando JdbcTemplate parametrizado.
 */
@Repository
public class AuditoriaDaoImpl implements IAuditoriaDao {

    private final JdbcTemplate jdbcTemplate;

    public AuditoriaDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertarRegistro(long idUsuario, String accion, String entidad, long idEntidad, String descripcion, String ip) {
        String sql = """
                INSERT INTO action_logs (user_id, accion, entidad, entidad_id, descripcion, ip_address, created_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                """;
        jdbcTemplate.update(sql, idUsuario, accion, entidad, idEntidad, descripcion, ip);
    }

    @Override
    public long contarHistorial(Long idUsuario, String accion, String entidad) {
        List<String> condiciones = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();
        construirCondiciones(condiciones, parametros, idUsuario, accion, entidad);

        String where = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
        String sql = "SELECT COUNT(*) FROM action_logs l" + where;
        Long total = jdbcTemplate.queryForObject(sql, Long.class, parametros.toArray());
        return total == null ? 0L : total;
    }

    @Override
    public List<Map<String, Object>> listarHistorial(Long idUsuario, String accion, String entidad, int limite, long desplazamiento) {
        List<String> condiciones = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();
        construirCondiciones(condiciones, parametros, idUsuario, accion, entidad);

        String where = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
        parametros.add(limite);
        parametros.add(desplazamiento);

        String sql = """
                SELECT l.id, l.user_id, l.accion, l.entidad, l.entidad_id,
                       l.descripcion, l.ip_address, l.created_at,
                       COALESCE(u.nombre, 'Sistema') AS usuario_nombre,
                       COALESCE(u.email, 'sistema@marcona.gob.pe') AS email
                FROM action_logs l
                LEFT JOIN users u ON l.user_id = u.id
                """ + where + " ORDER BY l.created_at DESC LIMIT ? OFFSET ?";

        return jdbcTemplate.queryForList(sql, parametros.toArray());
    }

    private void construirCondiciones(List<String> condiciones, List<Object> parametros,
                                      Long idUsuario, String accion, String entidad) {
        if (idUsuario != null) {
            condiciones.add("l.user_id = ?");
            parametros.add(idUsuario);
        }
        if (accion != null && !accion.isBlank()) {
            condiciones.add("l.accion = ?");
            parametros.add(accion.trim());
        }
        if (entidad != null && !entidad.isBlank()) {
            condiciones.add("l.entidad = ?");
            parametros.add(entidad.trim());
        }
    }
}
