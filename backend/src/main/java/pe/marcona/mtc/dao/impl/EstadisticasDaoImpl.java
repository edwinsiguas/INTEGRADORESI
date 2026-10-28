package pe.marcona.mtc.dao.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.marcona.mtc.dao.IEstadisticasDao;

import java.util.List;
import java.util.Map;

@Repository
public class EstadisticasDaoImpl implements IEstadisticasDao {

    private final JdbcTemplate jdbcTemplate;

    public EstadisticasDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long contarVehiculos() {
        return contar("SELECT COUNT(*) FROM vehicles");
    }

    @Override
    public long contarVehiculosPorEstado(String estado) {
        Long conteo = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vehicles WHERE estado = ?", Long.class, estado);
        return conteo == null ? 0L : conteo;
    }

    @Override
    public long contarEmpresas() {
        return contar("SELECT COUNT(*) FROM companies");
    }

    @Override
    public long contarEmpresasPorEstado(String estado) {
        Long conteo = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM companies WHERE estado = ?", Long.class, estado);
        return conteo == null ? 0L : conteo;
    }

    @Override
    public long contarConductores() {
        return contar("SELECT COUNT(*) FROM drivers");
    }

    @Override
    public long contarConductoresPorEstado(String estado) {
        Long conteo = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM drivers WHERE estado = ?", Long.class, estado);
        return conteo == null ? 0L : conteo;
    }

    @Override
    public long contarPermisosVigentes() {
        return contar("SELECT COUNT(*) FROM permits WHERE estado = 'vigente'");
    }

    @Override
    public long contarPermisosPorVencer(int dias) {
        Long conteo = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM permits
                WHERE estado = 'vigente'
                  AND fecha_vencimiento <= DATE_ADD(CURDATE(), INTERVAL ? DAY)
                """, Long.class, dias);
        return conteo == null ? 0L : conteo;
    }

    @Override
    public long contarVehiculosSoatVigente() {
        return contar("""
                SELECT COUNT(*) FROM vehicles
                WHERE estado = 'activo' AND soat_vencimiento >= CURDATE()
                """);
    }

    @Override
    public long contarVehiculosRtVigente() {
        return contar("""
                SELECT COUNT(*) FROM vehicles
                WHERE estado = 'activo' AND rt_vencimiento >= CURDATE()
                """);
    }

    @Override
    public long contarConductoresLicenciaVigente() {
        return contar("""
                SELECT COUNT(*) FROM drivers
                WHERE estado = 'activo' AND fecha_vencimiento_licencia >= CURDATE()
                """);
    }

    @Override
    public long contarTramitesMesActual() {
        return contar("""
                SELECT COUNT(*) FROM permits
                WHERE MONTH(created_at) = MONTH(CURDATE())
                  AND YEAR(created_at) = YEAR(CURDATE())
                """);
    }

    @Override
    public List<Map<String, Object>> obtenerTramitesPorMesUltimoSemestre() {
        return jdbcTemplate.query("""
                SELECT MONTH(created_at) AS month_number, COUNT(id) AS aprobados
                FROM permits
                WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
                GROUP BY MONTH(created_at)
                ORDER BY MONTH(created_at) ASC
                """, (rs, rowNum) -> Map.of(
                "month_number", rs.getInt("month_number"),
                "aprobados", rs.getLong("aprobados")));
    }

    @Override
    public List<Map<String, Object>> obtenerCrecimientoVehicularPorMes() {
        return jdbcTemplate.query("""
                SELECT DATE_FORMAT(created_at, '%Y-%m') AS month_key,
                       categoria, COUNT(id) AS total
                FROM vehicles
                WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 5 MONTH)
                GROUP BY month_key, categoria
                ORDER BY month_key ASC
                """, (rs, rowNum) -> Map.of(
                "month_key", rs.getString("month_key"),
                "categoria", rs.getString("categoria") == null ? "" : rs.getString("categoria"),
                "total", rs.getLong("total")));
    }

    @Override
    public List<Map<String, Object>> obtenerMesesSecuencia() {
        return jdbcTemplate.query("""
                SELECT DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL seq MONTH), '%Y-%m') AS month_key
                FROM (
                    SELECT 5 AS seq UNION ALL SELECT 4 UNION ALL SELECT 3
                    UNION ALL SELECT 2 UNION ALL SELECT 1 UNION ALL SELECT 0
                ) months
                ORDER BY seq DESC
                """, (rs, rowNum) -> Map.of(
                "month_key", rs.getString("month_key")));
    }

    @Override
    public List<Map<String, Object>> obtenerDistribucionTipoServicio() {
        return jdbcTemplate.query("""
                SELECT tipo_servicio, COUNT(id) AS total
                FROM vehicles
                WHERE estado = 'activo' AND tipo_servicio IS NOT NULL AND tipo_servicio <> ''
                GROUP BY tipo_servicio
                """, (rs, rowNum) -> Map.of(
                "tipo_servicio", rs.getString("tipo_servicio"),
                "total", rs.getLong("total")));
    }

    @Override
    public List<Map<String, Object>> obtenerRankingEmpresasFlota() {
        return jdbcTemplate.query("""
                SELECT c.id, c.nombre, c.ruc, COUNT(v.id) AS fleet,
                       SUM(CASE WHEN v.soat_vencimiento >= CURDATE() THEN 1 ELSE 0 END) AS valid_soat
                FROM companies c
                JOIN vehicles v ON v.company_id = c.id
                WHERE v.estado = 'activo'
                GROUP BY c.id, c.nombre, c.ruc
                ORDER BY fleet DESC
                LIMIT 5
                """, (rs, rowNum) -> Map.of(
                "id", rs.getLong("id"),
                "nombre", rs.getString("nombre") == null ? "" : rs.getString("nombre"),
                "ruc", rs.getString("ruc") == null ? "" : rs.getString("ruc"),
                "fleet", rs.getLong("fleet"),
                "valid_soat", rs.getLong("valid_soat")));
    }

    private long contar(String sql) {
        Long conteo = jdbcTemplate.queryForObject(sql, Long.class);
        return conteo == null ? 0L : conteo;
    }
}

