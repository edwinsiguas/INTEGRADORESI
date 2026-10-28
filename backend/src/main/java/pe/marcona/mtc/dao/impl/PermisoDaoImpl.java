package pe.marcona.mtc.dao.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import pe.marcona.mtc.dao.IPermisoDao;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación de persistencia para la entidad Permisos utilizando JdbcTemplate y consultas parametrizadas.
 */
@Repository
public class PermisoDaoImpl implements IPermisoDao {

    private final JdbcTemplate jdbcTemplate;

    public PermisoDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long contarPermisos(String busqueda, String estado, Long idVehiculo, Long idConductor,
                               Long idEmpresa, boolean proximoAVencer) {
        List<String> condiciones = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();
        construirCondiciones(condiciones, parametros, busqueda, estado, idVehiculo, idConductor, idEmpresa, proximoAVencer);

        String where = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
        String sql = """
                SELECT COUNT(*)
                FROM permits p
                JOIN vehicles v ON p.vehicle_id = v.id
                LEFT JOIN drivers d ON p.driver_id = d.id
                LEFT JOIN companies c ON p.company_id = c.id
                """ + where;

        Long total = jdbcTemplate.queryForObject(sql, Long.class, parametros.toArray());
        return total == null ? 0L : total;
    }

    @Override
    public List<Map<String, Object>> listarPermisos(String busqueda, String estado, Long idVehiculo, Long idConductor,
                                                   Long idEmpresa, boolean proximoAVencer, int limite, long desplazamiento) {
        List<String> condiciones = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();
        construirCondiciones(condiciones, parametros, busqueda, estado, idVehiculo, idConductor, idEmpresa, proximoAVencer);

        String where = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
        parametros.add(limite);
        parametros.add(desplazamiento);

        String sql = """
                SELECT p.id, p.numero_permiso, p.tipo_permiso, p.fecha_emision,
                       p.fecha_vencimiento, p.estado, p.observaciones, p.qr_code,
                       p.numero_recibo, p.monto_pagado, p.vehicle_id, p.driver_id, p.company_id,
                       v.placa, v.marca, v.modelo, v.categoria,
                       d.nombre_completo, d.dni,
                       d.numero_licencia, d.fecha_vencimiento_licencia,
                       c.nombre AS empresa
                FROM permits p
                JOIN vehicles v ON p.vehicle_id = v.id
                LEFT JOIN drivers d ON p.driver_id = d.id
                LEFT JOIN companies c ON p.company_id = c.id
                """ + where + " ORDER BY p.created_at DESC LIMIT ? OFFSET ?";

        return jdbcTemplate.queryForList(sql, parametros.toArray());
    }

    private void construirCondiciones(List<String> condiciones, List<Object> parametros,
                                      String busqueda, String estado, Long idVehiculo, Long idConductor,
                                      Long idEmpresa, boolean proximoAVencer) {
        if (busqueda != null && !busqueda.isBlank()) {
            condiciones.add("(p.numero_permiso LIKE ? OR d.nombre_completo LIKE ? OR v.placa LIKE ?)");
            String termino = "%" + busqueda.trim() + "%";
            parametros.add(termino);
            parametros.add(termino);
            parametros.add(termino);
        }
        if (estado != null && !estado.isBlank() && !"todos".equalsIgnoreCase(estado)) {
            condiciones.add("p.estado = ?");
            parametros.add(estado.trim());
        }
        if (idVehiculo != null) {
            condiciones.add("p.vehicle_id = ?");
            parametros.add(idVehiculo);
        }
        if (idConductor != null) {
            condiciones.add("p.driver_id = ?");
            parametros.add(idConductor);
        }
        if (idEmpresa != null) {
            condiciones.add("p.company_id = ?");
            parametros.add(idEmpresa);
        }
        if (proximoAVencer) {
            condiciones.add("p.estado IN ('vigente', 'vencido')");
            condiciones.add("p.fecha_vencimiento <= DATE_ADD(CURDATE(), INTERVAL 7 DAY)");
        }
    }

    @Override
    public Optional<Map<String, Object>> obtenerPorId(long id) {
        String sql = """
                SELECT p.*,
                       v.placa, v.marca, v.modelo, v.categoria,
                       v.soat_vencimiento, v.rt_vencimiento,
                       d.nombre_completo, d.dni,
                       d.numero_licencia, d.fecha_vencimiento_licencia,
                       c.nombre AS empresa
                FROM permits p
                JOIN vehicles v ON p.vehicle_id = v.id
                LEFT JOIN drivers d ON p.driver_id = d.id
                LEFT JOIN companies c ON p.company_id = c.id
                WHERE p.id = ?
                """;
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(sql, id);
        return filas.isEmpty() ? Optional.empty() : Optional.of(filas.get(0));
    }

    @Override
    public Optional<Map<String, Object>> obtenerVehiculoPorId(long idVehiculo) {
        String sql = "SELECT * FROM vehicles WHERE id = ?";
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(sql, idVehiculo);
        return filas.isEmpty() ? Optional.empty() : Optional.of(filas.get(0));
    }

    @Override
    public Optional<Map<String, Object>> obtenerAsignacionActivaPorVehiculo(long idVehiculo) {
        String sql = """
                SELECT va.*, d.nombre_completo, d.dni, d.fecha_vencimiento_licencia, d.estado AS estado_conductor
                FROM vehicle_assignments va
                JOIN drivers d ON va.driver_id = d.id
                WHERE va.vehicle_id = ? AND va.activo = 1
                """;
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(sql, idVehiculo);
        return filas.isEmpty() ? Optional.empty() : Optional.of(filas.get(0));
    }

    @Override
    public int contarPermisosVigentesPorAsignacion(long idAsignacion) {
        String sql = """
                SELECT COUNT(*) FROM permits
                WHERE assignment_id = ? AND estado = 'vigente' AND fecha_vencimiento >= CURDATE()
                """;
        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, idAsignacion);
        return total == null ? 0 : total;
    }

    @Override
    public Optional<Map<String, Object>> obtenerConductorPorId(long idConductor) {
        String sql = "SELECT * FROM drivers WHERE id = ?";
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(sql, idConductor);
        return filas.isEmpty() ? Optional.empty() : Optional.of(filas.get(0));
    }

    @Override
    public int contarAsignacionesActivasPorId(long idAsignacion) {
        String sql = "SELECT COUNT(*) FROM vehicle_assignments WHERE id = ? AND activo = 1";
        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, idAsignacion);
        return total == null ? 0 : total;
    }

    @Override
    public long insertarPermiso(Long idAsignacion, long idVehiculo, long idConductor, Long idEmpresa,
                                String numeroPermiso, String tipoPermiso, LocalDate fechaVencimiento,
                                String observaciones, String qrCode, long idCreador,
                                String numeroRecibo, BigDecimal montoPagado) {
        String sql = """
                INSERT INTO permits
                    (assignment_id, vehicle_id, driver_id, company_id, numero_permiso,
                     tipo_permiso, fecha_emision, fecha_vencimiento, observaciones, qr_code,
                     estado, created_by, numero_recibo, monto_pagado)
                VALUES (?, ?, ?, ?, ?, ?, CURDATE(), ?, ?, ?, 'vigente', ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (idAsignacion != null) ps.setLong(1, idAsignacion); else ps.setNull(1, java.sql.Types.BIGINT);
            ps.setLong(2, idVehiculo);
            ps.setLong(3, idConductor);
            if (idEmpresa != null) ps.setLong(4, idEmpresa); else ps.setNull(4, java.sql.Types.BIGINT);
            ps.setString(5, numeroPermiso);
            ps.setString(6, tipoPermiso);
            ps.setDate(7, Date.valueOf(fechaVencimiento));
            ps.setString(8, observaciones);
            ps.setString(9, qrCode);
            ps.setLong(10, idCreador);
            ps.setString(11, numeroRecibo);
            if (montoPagado != null) ps.setBigDecimal(12, montoPagado); else ps.setNull(12, java.sql.Types.DECIMAL);
            return ps;
        }, keyHolder);

        Number claveGenerada = keyHolder.getKey();
        if (claveGenerada == null) {
            throw new IllegalStateException("No se pudo obtener el identificador generado del permiso");
        }
        return claveGenerada.longValue();
    }

    @Override
    public boolean revocarPermiso(long idPermiso, String motivo) {
        String sql = """
                UPDATE permits
                SET estado = 'revocado',
                    observaciones = CONCAT(COALESCE(observaciones, ''), ' | REVOCADO: ', ?)
                WHERE id = ? AND estado = 'vigente'
                """;
        int filasModificadas = jdbcTemplate.update(sql, motivo, idPermiso);
        return filasModificadas > 0;
    }

    @Override
    public int actualizarEstado(long idPermiso, String nuevoEstado) {
        String sql = "UPDATE permits SET estado = ? WHERE id = ?";
        return jdbcTemplate.update(sql, nuevoEstado, idPermiso);
    }

    @Override
    public List<Map<String, Object>> obtenerImagenesVehiculo(long idVehiculo) {
        String sql = """
                SELECT file_path, sort_order
                FROM vehicle_images
                WHERE vehicle_id = ?
                ORDER BY sort_order ASC, id ASC
                LIMIT 2
                """;
        return jdbcTemplate.queryForList(sql, idVehiculo);
    }

    @Override
    public List<Long> obtenerIdsPermisosVencidosParaBloqueo() {
        String sql = """
                SELECT id FROM permits
                WHERE estado = 'vigente' AND fecha_vencimiento < CURDATE()
                FOR UPDATE
                """;
        return jdbcTemplate.queryForList(sql, Long.class);
    }

    @Override
    public int marcarComoVencido(long idPermiso) {
        String sql = "UPDATE permits SET estado = 'vencido' WHERE id = ?";
        return jdbcTemplate.update(sql, idPermiso);
    }

    @Override
    public int obtenerSiguienteSecuencia(int anio) {
        jdbcTemplate.update("INSERT IGNORE INTO permit_sequences (year, last_number) VALUES (?, 0)", anio);
        jdbcTemplate.update("UPDATE permit_sequences SET last_number = last_number + 1 WHERE year = ?", anio);
        Integer secuencia = jdbcTemplate.queryForObject(
                "SELECT last_number FROM permit_sequences WHERE year = ?", Integer.class, anio);
        return secuencia == null ? 1 : secuencia;
    }
}

