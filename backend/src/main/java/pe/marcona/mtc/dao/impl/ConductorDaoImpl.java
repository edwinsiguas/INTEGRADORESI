package pe.marcona.mtc.dao.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.marcona.mtc.dao.IConductorDao;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Implementación de persistencia para la entidad Conductores utilizando consultas parametrizadas.
 */
@Repository
public class ConductorDaoImpl implements IConductorDao {

    private final JdbcTemplate jdbcTemplate;

    public ConductorDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long contarConductores(String busqueda, Long idEmpresa, String estado, boolean incluirInactivos) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        construirFiltros(where, args, busqueda, idEmpresa, estado, incluirInactivos);

        String sql = "SELECT COUNT(*) FROM drivers d LEFT JOIN companies c ON d.company_id = c.id" + where;
        Long total = jdbcTemplate.queryForObject(sql, Long.class, args.toArray());
        return total == null ? 0L : total;
    }

    @Override
    public List<Map<String, Object>> listarConductores(String busqueda, Long idEmpresa, String estado,
                                                       boolean incluirInactivos, int limite, long desplazamiento) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        construirFiltros(where, args, busqueda, idEmpresa, estado, incluirInactivos);

        args.add(limite);
        args.add(desplazamiento);

        String sql = """
                SELECT d.*, c.nombre AS company_nombre
                FROM drivers d
                LEFT JOIN companies c ON d.company_id = c.id
                """ + where + " ORDER BY d.created_at DESC LIMIT ? OFFSET ?";

        return jdbcTemplate.queryForList(sql, args.toArray());
    }

    private void construirFiltros(StringBuilder where, List<Object> args,
                                  String busqueda, Long idEmpresa, String estado, boolean incluirInactivos) {
        if (busqueda != null && !busqueda.isBlank()) {
            where.append(" AND (d.nombre_completo LIKE ? OR d.dni LIKE ? OR d.numero_licencia LIKE ?)");
            String termino = "%" + busqueda.trim() + "%";
            args.add(termino);
            args.add(termino);
            args.add(termino);
        }
        if (idEmpresa != null) {
            where.append(" AND d.company_id = ?");
            args.add(idEmpresa);
        }
        if (estado != null && !estado.isBlank()) {
            where.append(" AND d.estado = ?");
            args.add(estado.trim());
        }
        if (!incluirInactivos && (estado == null || estado.isBlank())) {
            where.append(" AND d.estado = 'activo'");
        }
    }

    @Override
    public Optional<Map<String, Object>> obtenerPorId(long id) {
        String sql = "SELECT * FROM drivers WHERE id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public Optional<Map<String, Object>> obtenerPorDni(String dni) {
        String sql = "SELECT * FROM drivers WHERE dni = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, dni);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public boolean existePorColumna(String columna, Object valor, Long idExcluido) {
        if (!Set.of("dni", "numero_licencia").contains(columna)) {
            throw new IllegalArgumentException("Columna de unicidad no permitida: " + columna);
        }
        String sql = idExcluido == null
                ? "SELECT COUNT(*) FROM drivers WHERE " + columna + " = ?"
                : "SELECT COUNT(*) FROM drivers WHERE " + columna + " = ? AND id <> ?";
        Integer count = idExcluido == null
                ? jdbcTemplate.queryForObject(sql, Integer.class, valor)
                : jdbcTemplate.queryForObject(sql, Integer.class, valor, idExcluido);
        return count != null && count > 0;
    }

    @Override
    public void insertarConductor(Long idUsuario, Long idEmpresa, String nombreCompleto, String dni,
                                  String telefono, String numeroLicencia, LocalDate fechaVencimientoLicencia) {
        String sql = """
                INSERT INTO drivers
                    (user_id, company_id, nombre_completo, dni, telefono,
                     numero_licencia, fecha_vencimiento_licencia, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'activo')
                """;
        jdbcTemplate.update(sql, idUsuario, idEmpresa, nombreCompleto, dni, telefono,
                numeroLicencia, Date.valueOf(fechaVencimientoLicencia));
    }

    @Override
    public int actualizarConductor(long id, Map<String, Object> campos) {
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

        String sql = "UPDATE drivers SET " + String.join(", ", clausulas) + " WHERE id = ?";
        return jdbcTemplate.update(sql, valores.toArray());
    }
}
