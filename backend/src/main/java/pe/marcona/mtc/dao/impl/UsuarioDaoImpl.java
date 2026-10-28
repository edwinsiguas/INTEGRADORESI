package pe.marcona.mtc.dao.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.marcona.mtc.dao.IUsuarioDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación de persistencia para la entidad Usuarios utilizando JdbcTemplate con consultas parametrizadas.
 */
@Repository
public class UsuarioDaoImpl implements IUsuarioDao {

    private final JdbcTemplate jdbcTemplate;

    public UsuarioDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long contarUsuarios(String busqueda, String rol, String estado, boolean incluirInactivos) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();

        construirFiltros(where, parametros, busqueda, rol, estado, incluirInactivos);

        String sql = "SELECT COUNT(*) FROM users" + where;
        Long total = jdbcTemplate.queryForObject(sql, Long.class, parametros.toArray());
        return total == null ? 0L : total;
    }

    @Override
    public List<Map<String, Object>> listarUsuarios(String busqueda, String rol, String estado,
                                                    boolean incluirInactivos, int limite, long desplazamiento) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();

        construirFiltros(where, parametros, busqueda, rol, estado, incluirInactivos);

        parametros.add(limite);
        parametros.add(desplazamiento);

        String sql = """
                SELECT id, email, nombre, role, estado, is_active, created_at
                FROM users
                """ + where + " ORDER BY created_at DESC LIMIT ? OFFSET ?";

        return jdbcTemplate.queryForList(sql, parametros.toArray());
    }

    private void construirFiltros(StringBuilder where, List<Object> parametros,
                                  String busqueda, String rol, String estado, boolean incluirInactivos) {
        if (busqueda != null && !busqueda.isBlank()) {
            where.append(" AND (email LIKE ? OR nombre LIKE ?)");
            parametros.add("%" + busqueda + "%");
            parametros.add("%" + busqueda + "%");
        }
        if (rol != null && !rol.isBlank()) {
            where.append(" AND role = ?");
            parametros.add(rol);
        }
        if (estado != null && !estado.isBlank()) {
            where.append(" AND estado = ?");
            parametros.add(estado);
        }
        if (!incluirInactivos && (estado == null || estado.isBlank())) {
            where.append(" AND is_active = 1");
        }
    }

    @Override
    public void insertarUsuario(String email, String passwordEncriptada, String nombre, String rol) {
        String sql = """
                INSERT INTO users (email, password, nombre, role, estado, is_active)
                VALUES (?, ?, ?, ?, 'activo', 1)
                """;
        jdbcTemplate.update(sql, email, passwordEncriptada, nombre, rol);
    }

    @Override
    public Optional<Map<String, Object>> obtenerPorEmail(String email) {
        String sql = """
                SELECT id, email, nombre, role, estado, is_active, created_at
                FROM users WHERE email = ?
                """;
        List<Map<String, Object>> resultado = jdbcTemplate.queryForList(sql, email);
        return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
    }

    @Override
    public Optional<Map<String, Object>> obtenerPorId(Long id) {
        String sql = """
                SELECT id, email, nombre, role, estado, is_active, created_at, updated_at
                FROM users WHERE id = ?
                """;
        List<Map<String, Object>> resultado = jdbcTemplate.queryForList(sql, id);
        return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
    }

    @Override
    public boolean existePorEmail(String email, Long idExcluido) {
        String sql;
        Integer conteo;
        if (idExcluido == null) {
            sql = "SELECT COUNT(*) FROM users WHERE email = ?";
            conteo = jdbcTemplate.queryForObject(sql, Integer.class, email);
        } else {
            sql = "SELECT COUNT(*) FROM users WHERE email = ? AND id <> ?";
            conteo = jdbcTemplate.queryForObject(sql, Integer.class, email, idExcluido);
        }
        return conteo != null && conteo > 0;
    }

    @Override
    public int actualizarCampos(Long id, Map<String, Object> campos) {
        if (campos == null || campos.isEmpty()) {
            return 0;
        }
        List<String> clausulas = new ArrayList<>();
        List<Object> valores = new ArrayList<>();

        for (Map.Entry<String, Object> entrada : campos.entrySet()) {
            clausulas.add(entrada.getKey() + " = ?");
            valores.add(entrada.getValue());
        }
        clausulas.add("updated_at = NOW()");
        valores.add(id);

        String sql = "UPDATE users SET " + String.join(", ", clausulas) + " WHERE id = ?";
        return jdbcTemplate.update(sql, valores.toArray());
    }
}
