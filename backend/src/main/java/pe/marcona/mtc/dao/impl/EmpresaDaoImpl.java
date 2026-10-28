package pe.marcona.mtc.dao.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.marcona.mtc.dao.IEmpresaDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Implementación de persistencia para Empresas con JdbcTemplate y consultas parametrizadas.
 */
@Repository
public class EmpresaDaoImpl implements IEmpresaDao {

    private final JdbcTemplate jdbcTemplate;

    public EmpresaDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long contarEmpresas(String busqueda, String estado) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        construirFiltros(where, args, busqueda, estado);

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM companies c" + where, Long.class, args.toArray());
        return total == null ? 0L : total;
    }

    @Override
    public long contarEmpresasActivas() {
        Long conteo = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM companies WHERE estado = 'activo'", Long.class);
        return conteo == null ? 0L : conteo;
    }

    @Override
    public List<Map<String, Object>> listarEmpresas(String busqueda, String estado, int limite, long desplazamiento) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        construirFiltros(where, args, busqueda, estado);

        args.add(limite);
        args.add(desplazamiento);

        String sql = """
                SELECT c.*, COUNT(v.id) AS flota
                FROM companies c
                LEFT JOIN vehicles v ON v.company_id = c.id AND v.estado = 'activo'
                """ + where + " GROUP BY c.id ORDER BY c.created_at DESC LIMIT ? OFFSET ?";

        return jdbcTemplate.queryForList(sql, args.toArray());
    }

    private void construirFiltros(StringBuilder where, List<Object> args, String busqueda, String estado) {
        if (busqueda != null && !busqueda.isBlank()) {
            where.append(" AND (c.nombre LIKE ? OR c.ruc LIKE ?)");
            String termino = "%" + busqueda.trim() + "%";
            args.add(termino);
            args.add(termino);
        }
        if (estado != null && !estado.isBlank()) {
            where.append(" AND c.estado = ?");
            args.add(estado.trim());
        }
    }

    @Override
    public Optional<Map<String, Object>> obtenerPorId(long id) {
        String sql = "SELECT * FROM companies WHERE id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public Optional<Map<String, Object>> obtenerPorRuc(String ruc) {
        String sql = "SELECT * FROM companies WHERE ruc = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, ruc);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public boolean existePorColumna(String columna, Object valor, Long idExcluido) {
        if (!Set.of("nombre", "ruc").contains(columna)) {
            throw new IllegalArgumentException("Columna de unicidad no permitida: " + columna);
        }
        String sql = idExcluido == null
                ? "SELECT COUNT(*) FROM companies WHERE " + columna + " = ?"
                : "SELECT COUNT(*) FROM companies WHERE " + columna + " = ? AND id <> ?";
        Integer count = idExcluido == null
                ? jdbcTemplate.queryForObject(sql, Integer.class, valor)
                : jdbcTemplate.queryForObject(sql, Integer.class, valor, idExcluido);
        return count != null && count > 0;
    }

    @Override
    public void insertarEmpresa(String nombre, String ruc, String telefono, String email, String direccion, String contacto) {
        String sql = """
                INSERT INTO companies (nombre, ruc, telefono, email, direccion, contacto, estado)
                VALUES (?, ?, ?, ?, ?, ?, 'activo')
                """;
        jdbcTemplate.update(sql, nombre, ruc, telefono, email, direccion, contacto);
    }

    @Override
    public int actualizarEmpresa(long id, String nombre, String ruc, String telefono, String email,
                                 String direccion, String contacto, String estado) {
        String sql = """
                UPDATE companies
                SET nombre = ?, ruc = ?, telefono = ?, email = ?, direccion = ?, contacto = ?, estado = ?
                WHERE id = ?
                """;
        return jdbcTemplate.update(sql, nombre, ruc, telefono, email, direccion, contacto, estado, id);
    }

    @Override
    public int actualizarEstado(long id, String nuevoEstado) {
        String sql = "UPDATE companies SET estado = ? WHERE id = ?";
        return jdbcTemplate.update(sql, nuevoEstado, id);
    }
}

