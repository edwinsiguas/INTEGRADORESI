package pe.marcona.mtc.dao.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.marcona.mtc.dao.IExportacionDao;

import java.util.List;
import java.util.Map;

@Repository
public class ExportacionDaoImpl implements IExportacionDao {

    private final JdbcTemplate jdbcTemplate;

    public ExportacionDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> consultarEmpresasParaExportar(String condicionWhere, List<Object> parametros) {
        return jdbcTemplate.queryForList("""
                SELECT 
                    c.id,
                    c.nombre AS razon_social,
                    c.ruc,
                    c.contacto AS representante_legal,
                    c.telefono,
                    c.email,
                    c.direccion,
                    c.estado,
                    DATE(c.created_at) AS fecha_registro,
                    (SELECT COUNT(*) FROM vehicles v WHERE v.company_id = c.id AND v.estado = 'activo') AS total_vehiculos,
                    (SELECT COUNT(*) FROM vehicles v WHERE v.company_id = c.id AND v.estado = 'activo' AND (v.soat_vencimiento IS NULL OR v.soat_vencimiento < CURDATE())) AS vehiculos_soat_vencido,
                    (SELECT COUNT(*) FROM vehicles v WHERE v.company_id = c.id AND v.estado = 'activo' AND (v.rt_vencimiento IS NULL OR v.rt_vencimiento < CURDATE())) AS vehiculos_rt_vencida,
                    (SELECT COUNT(*) FROM drivers d WHERE d.company_id = c.id AND d.estado = 'activo') AS total_conductores,
                    (SELECT COUNT(*) FROM drivers d WHERE d.company_id = c.id AND d.estado = 'activo' AND (d.fecha_vencimiento_licencia IS NULL OR d.fecha_vencimiento_licencia < CURDATE())) AS conductores_brevete_vencido,
                    (SELECT COUNT(*) FROM permits p WHERE p.company_id = c.id AND p.estado = 'vigente') AS tucs_vigentes,
                    (SELECT COUNT(*) FROM permits p WHERE p.company_id = c.id AND p.estado = 'vencido') AS tucs_vencidos
                FROM companies c
                """ + condicionWhere + " ORDER BY c.estado ASC, c.nombre ASC", parametros.toArray());
    }

    @Override
    public List<Map<String, Object>> consultarConductoresParaExportar(String condicionWhere, List<Object> parametros) {
        return jdbcTemplate.queryForList("""
                SELECT 
                    d.id, 
                    d.nombre_completo, 
                    d.dni, 
                    d.telefono, 
                    d.numero_licencia, 
                    d.fecha_vencimiento_licencia, 
                    d.estado,
                    v.placa AS vehiculo_asignado,
                    c.nombre AS empresa
                FROM drivers d
                LEFT JOIN vehicle_assignments va ON va.driver_id = d.id AND va.activo = 1
                LEFT JOIN vehicles v ON va.vehicle_id = v.id
                LEFT JOIN companies c ON d.company_id = c.id
                """ + condicionWhere + " ORDER BY d.estado ASC, d.nombre_completo ASC", parametros.toArray());
    }

    @Override
    public List<Map<String, Object>> consultarVehiculosParaExportar(String condicionWhere, List<Object> parametros) {
        return jdbcTemplate.queryForList("""
                SELECT 
                    v.id, 
                    v.placa, 
                    v.marca, 
                    v.modelo, 
                    v.color, 
                    v.ano_fabricacion,
                    v.ano_fabricacion AS anio_fabricacion, 
                    v.numero_vin,
                    v.soat_vencimiento, 
                    v.rt_vencimiento, 
                    v.estado,
                    d.nombre_completo AS conductor_asignado, 
                    c.nombre AS empresa
                FROM vehicles v
                LEFT JOIN vehicle_assignments va ON va.vehicle_id = v.id AND va.activo = 1
                LEFT JOIN drivers d ON va.driver_id = d.id
                LEFT JOIN companies c ON v.company_id = c.id
                """ + condicionWhere + " ORDER BY v.estado ASC, c.nombre ASC, v.placa ASC", parametros.toArray());
    }

    @Override
    public List<Map<String, Object>> consultarPermisosParaExportar(String condicionWhere, List<Object> parametros) {
        return jdbcTemplate.queryForList("""
                SELECT 
                    p.id, 
                    p.numero_permiso, 
                    p.fecha_emision, 
                    p.fecha_vencimiento, 
                    p.estado,
                    v.placa, 
                    v.marca, 
                    v.modelo, 
                    v.soat_vencimiento, 
                    v.rt_vencimiento,
                    d.nombre_completo AS conductor, 
                    d.dni AS conductor_dni,
                    c.nombre AS empresa
                FROM permits p
                JOIN vehicles v ON p.vehicle_id = v.id
                LEFT JOIN drivers d ON p.driver_id = d.id
                LEFT JOIN companies c ON p.company_id = c.id
                """ + condicionWhere + " ORDER BY p.created_at DESC", parametros.toArray());
    }
}
