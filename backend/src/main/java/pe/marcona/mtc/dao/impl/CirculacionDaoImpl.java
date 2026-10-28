package pe.marcona.mtc.dao.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.marcona.mtc.dao.ICirculacionDao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CirculacionDaoImpl implements ICirculacionDao {

    private final JdbcTemplate jdbcTemplate;

    public CirculacionDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Map<String, Object>> buscarPorPlaca(String placa) {
        List<Map<String, Object>> filas = jdbcTemplate.queryForList("""
                SELECT v.id AS vehicle_id, v.placa, v.marca, v.modelo, v.color,
                       v.estado AS estado_vehiculo, v.tipo_propiedad,
                       v.soat_vencimiento, v.rt_vencimiento,
                       va.id AS assignment_id, va.tipo AS tipo_asignacion,
                       d.id AS driver_id, d.nombre_completo, d.dni, d.numero_licencia,
                       d.fecha_vencimiento_licencia, d.estado AS estado_conductor,
                       p.id AS permit_id, p.numero_permiso, p.tipo_permiso, p.qr_code,
                       p.fecha_emision, p.fecha_vencimiento AS permiso_vence,
                       p.estado AS estado_permiso, c.nombre AS empresa, c.estado AS estado_empresa
                FROM vehicles v
                JOIN vehicle_assignments va ON va.vehicle_id = v.id AND va.activo = 1
                JOIN drivers d ON va.driver_id = d.id
                LEFT JOIN permits p ON p.vehicle_id = v.id AND p.estado IN ('vigente', 'vencido')
                LEFT JOIN companies c ON v.company_id = c.id
                WHERE v.placa = ?
                ORDER BY p.fecha_vencimiento DESC
                LIMIT 1
                """, placa.trim().toUpperCase());
        return filas.isEmpty() ? Optional.empty() : Optional.of(filas.get(0));
    }

    @Override
    public Optional<Map<String, Object>> buscarPorDni(String dni) {
        List<Map<String, Object>> filas = jdbcTemplate.queryForList("""
                SELECT v.id AS vehicle_id, v.placa, v.marca, v.modelo,
                       v.estado AS estado_vehiculo, v.soat_vencimiento, v.rt_vencimiento,
                       va.id AS assignment_id, va.tipo AS tipo_asignacion,
                       d.id AS driver_id, d.nombre_completo, d.dni, d.numero_licencia,
                       d.fecha_vencimiento_licencia, d.estado AS estado_conductor,
                       p.id AS permit_id, p.numero_permiso, p.tipo_permiso, p.qr_code,
                       p.fecha_emision, p.fecha_vencimiento AS permiso_vence,
                       p.estado AS estado_permiso, c.nombre AS empresa, c.estado AS estado_empresa
                FROM drivers d
                JOIN vehicle_assignments va ON va.driver_id = d.id AND va.activo = 1
                JOIN vehicles v ON va.vehicle_id = v.id
                LEFT JOIN permits p ON p.vehicle_id = v.id AND p.estado IN ('vigente', 'vencido')
                LEFT JOIN companies c ON v.company_id = c.id
                WHERE d.dni = ?
                ORDER BY p.fecha_vencimiento DESC
                LIMIT 1
                """, dni.trim());
        return filas.isEmpty() ? Optional.empty() : Optional.of(filas.get(0));
    }

    @Override
    public Optional<Map<String, Object>> buscarPorNumeroPermiso(String numeroPermiso) {
        String num = numeroPermiso.trim().toUpperCase();
        List<Map<String, Object>> filas = jdbcTemplate.queryForList("""
                SELECT v.id AS vehicle_id, v.placa, v.marca, v.modelo,
                       v.estado AS estado_vehiculo, v.soat_vencimiento, v.rt_vencimiento,
                       va.id AS assignment_id, d.id AS driver_id, d.nombre_completo, d.dni,
                       d.numero_licencia, d.fecha_vencimiento_licencia,
                       d.estado AS estado_conductor, p.id AS permit_id, p.numero_permiso,
                       p.tipo_permiso, p.qr_code, p.fecha_emision,
                       p.fecha_vencimiento AS permiso_vence, p.estado AS estado_permiso,
                       c.nombre AS empresa, c.estado AS estado_empresa
                FROM permits p
                JOIN vehicle_assignments va ON p.assignment_id = va.id
                JOIN drivers d ON va.driver_id = d.id
                JOIN vehicles v ON va.vehicle_id = v.id
                LEFT JOIN companies c ON v.company_id = c.id
                WHERE p.numero_permiso = ?
                """, num);

        if (filas.isEmpty()) {
            filas = jdbcTemplate.queryForList("""
                    SELECT v.id AS vehicle_id, v.placa, v.marca, v.modelo,
                           v.estado AS estado_vehiculo, v.soat_vencimiento, v.rt_vencimiento,
                           d.id AS driver_id, d.nombre_completo, d.dni, d.numero_licencia,
                           d.fecha_vencimiento_licencia, d.estado AS estado_conductor,
                           p.id AS permit_id, p.numero_permiso, p.tipo_permiso, p.qr_code,
                           p.fecha_vencimiento AS permiso_vence, p.estado AS estado_permiso,
                           c.nombre AS empresa, c.estado AS estado_empresa
                    FROM permits p
                    JOIN vehicles v ON p.vehicle_id = v.id
                    LEFT JOIN drivers d ON p.driver_id = d.id
                    LEFT JOIN companies c ON p.company_id = c.id
                    WHERE p.numero_permiso = ?
                    """, num);
        }
        return filas.isEmpty() ? Optional.empty() : Optional.of(filas.get(0));
    }
}
