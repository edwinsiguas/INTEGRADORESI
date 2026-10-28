package pe.marcona.mtc.dao.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import pe.marcona.mtc.dao.IAsignacionDao;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación de persistencia para Asignaciones utilizando consultas parametrizadas.
 */
@Repository
public class AsignacionDaoImpl implements IAsignacionDao {

    private final JdbcTemplate jdbcTemplate;

    public AsignacionDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long contarAsignaciones(Long idVehiculo, Long idConductor, String tipo, Integer activo) {
        List<String> condiciones = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();
        construirCondiciones(condiciones, parametros, idVehiculo, idConductor, tipo, activo);

        String where = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
        String sql = "SELECT COUNT(*) FROM vehicle_assignments va" + where;
        Long total = jdbcTemplate.queryForObject(sql, Long.class, parametros.toArray());
        return total == null ? 0L : total;
    }

    @Override
    public List<Map<String, Object>> listarAsignaciones(Long idVehiculo, Long idConductor, String tipo,
                                                        Integer activo, int limite, long desplazamiento) {
        List<String> condiciones = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();
        construirCondiciones(condiciones, parametros, idVehiculo, idConductor, tipo, activo);

        String where = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
        parametros.add(limite);
        parametros.add(desplazamiento);

        String sql = """
                SELECT va.*,
                       d.nombre_completo, d.dni, d.numero_licencia,
                       d.fecha_vencimiento_licencia, d.estado AS estado_conductor,
                       v.placa, v.marca, v.modelo, v.estado AS estado_vehiculo,
                       c.nombre AS empresa
                FROM vehicle_assignments va
                JOIN drivers d ON va.driver_id = d.id
                JOIN vehicles v ON va.vehicle_id = v.id
                LEFT JOIN companies c ON v.company_id = c.id
                """ + where + " ORDER BY va.created_at DESC LIMIT ? OFFSET ?";

        return jdbcTemplate.queryForList(sql, parametros.toArray());
    }

    private void construirCondiciones(List<String> condiciones, List<Object> parametros,
                                      Long idVehiculo, Long idConductor, String tipo, Integer activo) {
        if (idVehiculo != null) {
            condiciones.add("va.vehicle_id = ?");
            parametros.add(idVehiculo);
        }
        if (idConductor != null) {
            condiciones.add("va.driver_id = ?");
            parametros.add(idConductor);
        }
        if (tipo != null && !tipo.isBlank()) {
            condiciones.add("va.tipo = ?");
            parametros.add(tipo.trim());
        }
        if (activo != null) {
            condiciones.add("va.activo = ?");
            parametros.add(activo);
        }
    }

    @Override
    public Optional<Map<String, Object>> obtenerVehiculoPorId(long idVehiculo) {
        String sql = "SELECT id, estado FROM vehicles WHERE id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, idVehiculo);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public Optional<Map<String, Object>> obtenerConductorPorId(long idConductor) {
        String sql = "SELECT id, estado, fecha_vencimiento_licencia FROM drivers WHERE id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, idConductor);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public int desactivarAsignacionActivaPorVehiculo(long idVehiculo) {
        String sql = """
                UPDATE vehicle_assignments
                SET activo = 0, fecha_fin = CURDATE()
                WHERE vehicle_id = ? AND activo = 1
                """;
        return jdbcTemplate.update(sql, idVehiculo);
    }

    @Override
    public long insertarAsignacion(long idVehiculo, long idConductor, String tipo, String propietarioNombre,
                                   String propietarioDni, String fechaFin, String notas, long idCreador) {
        String sql = """
                INSERT INTO vehicle_assignments
                    (vehicle_id, driver_id, tipo, propietario_nombre, propietario_dni,
                     fecha_inicio, fecha_fin, activo, notas, created_by)
                VALUES (?, ?, ?, ?, ?, CURDATE(), ?, 1, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, idVehiculo);
            ps.setLong(2, idConductor);
            ps.setString(3, tipo);
            ps.setString(4, propietarioNombre);
            ps.setString(5, propietarioDni);
            ps.setString(6, fechaFin);
            ps.setString(7, notas);
            ps.setLong(8, idCreador);
            return ps;
        }, keyHolder);

        Number clave = keyHolder.getKey();
        if (clave == null) {
            throw new IllegalStateException("No se pudo recuperar la asignación recién creada");
        }
        return clave.longValue();
    }

    @Override
    public Optional<Map<String, Object>> obtenerPorId(long id) {
        String sql = """
                SELECT va.*, d.nombre_completo, d.dni, d.numero_licencia,
                       d.fecha_vencimiento_licencia, v.placa, v.marca, v.modelo,
                       c.nombre AS empresa
                FROM vehicle_assignments va
                JOIN drivers d ON va.driver_id = d.id
                JOIN vehicles v ON va.vehicle_id = v.id
                LEFT JOIN companies c ON v.company_id = c.id
                WHERE va.id = ?
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public Optional<Map<String, Object>> obtenerActivaPorVehiculo(long idVehiculo) {
        String sql = """
                SELECT va.*, d.nombre_completo, d.dni, d.telefono, d.numero_licencia,
                       d.categoria_licencia, d.fecha_vencimiento_licencia,
                       d.estado AS estado_conductor, v.placa, v.marca, v.modelo, v.color,
                       v.estado AS estado_vehiculo, v.tipo_propiedad, c.nombre AS empresa
                FROM vehicle_assignments va
                JOIN drivers d ON va.driver_id = d.id
                JOIN vehicles v ON va.vehicle_id = v.id
                LEFT JOIN companies c ON v.company_id = c.id
                WHERE va.vehicle_id = ? AND va.activo = 1
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, idVehiculo);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public Optional<Map<String, Object>> obtenerActivaPorConductor(long idConductor) {
        String sql = """
                SELECT va.*, v.placa, v.marca, v.modelo, v.color,
                       v.estado AS estado_vehiculo, c.nombre AS empresa
                FROM vehicle_assignments va
                JOIN vehicles v ON va.vehicle_id = v.id
                LEFT JOIN companies c ON v.company_id = c.id
                WHERE va.driver_id = ? AND va.activo = 1
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, idConductor);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<Map<String, Object>> obtenerHistorialPorVehiculo(long idVehiculo) {
        String sql = """
                SELECT va.*, d.nombre_completo, d.dni, d.numero_licencia
                FROM vehicle_assignments va
                JOIN drivers d ON va.driver_id = d.id
                WHERE va.vehicle_id = ?
                ORDER BY va.created_at DESC
                """;
        return jdbcTemplate.queryForList(sql, idVehiculo);
    }

    @Override
    public int desasignarVehiculo(long idVehiculo) {
        String sql = """
                UPDATE vehicle_assignments
                SET activo = 0, fecha_fin = CURDATE()
                WHERE vehicle_id = ? AND activo = 1
                """;
        return jdbcTemplate.update(sql, idVehiculo);
    }
}

