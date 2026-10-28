package pe.marcona.mtc.dao.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.marcona.mtc.dao.IVehiculoConsultaDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementación de persistencia de solo lectura para vehículos utilizando consultas parametrizadas.
 */
@Repository
public class VehiculoConsultaDaoImpl implements IVehiculoConsultaDao {

    private final JdbcTemplate jdbcTemplate;

    public VehiculoConsultaDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long contarVehiculos(String busqueda, Long idEmpresa, Long idConductor, String estado, boolean incluirInactivos) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        construirFiltros(where, args, busqueda, idEmpresa, idConductor, estado, incluirInactivos);

        String sql = "SELECT COUNT(*) FROM vehicles v" + where;
        Long total = jdbcTemplate.queryForObject(sql, Long.class, args.toArray());
        return total == null ? 0L : total;
    }

    @Override
    public List<Map<String, Object>> listarVehiculos(String busqueda, Long idEmpresa, Long idConductor,
                                                     String estado, boolean incluirInactivos, int limite, long desplazamiento) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        construirFiltros(where, args, busqueda, idEmpresa, idConductor, estado, incluirInactivos);

        args.add(limite);
        args.add(desplazamiento);

        String sql = """
                SELECT v.*,
                       d.nombre_completo AS driver_nombre,
                       d.numero_licencia AS driver_licencia,
                       d.estado AS driver_estado,
                       va.tipo AS assignment_tipo,
                       va.id AS assignment_id,
                       c.nombre AS company_nombre,
                       (SELECT COUNT(*) FROM vehicle_images vi WHERE vi.vehicle_id = v.id) AS images_count,
                       (SELECT vi.file_path
                        FROM vehicle_images vi
                        WHERE vi.vehicle_id = v.id
                        ORDER BY vi.sort_order ASC, vi.id ASC
                        LIMIT 1) AS primary_image
                FROM vehicles v
                LEFT JOIN vehicle_assignments va ON va.vehicle_id = v.id AND va.activo = 1
                LEFT JOIN drivers d ON va.driver_id = d.id
                LEFT JOIN companies c ON v.company_id = c.id
                """ + where + " ORDER BY v.created_at DESC LIMIT ? OFFSET ?";

        return jdbcTemplate.queryForList(sql, args.toArray());
    }

    private void construirFiltros(StringBuilder where, List<Object> args,
                                  String busqueda, Long idEmpresa, Long idConductor,
                                  String estado, boolean incluirInactivos) {
        if (busqueda != null && !busqueda.isBlank()) {
            where.append(" AND (v.placa LIKE ? OR v.marca LIKE ? OR v.modelo LIKE ?)");
            String termino = "%" + busqueda.trim() + "%";
            args.add(termino);
            args.add(termino);
            args.add(termino);
        }
        if (idEmpresa != null) {
            where.append(" AND v.company_id = ?");
            args.add(idEmpresa);
        }
        if (idConductor != null) {
            where.append("""
                     AND EXISTS (
                         SELECT 1 FROM vehicle_assignments filter_assignment
                         WHERE filter_assignment.vehicle_id = v.id
                           AND filter_assignment.driver_id = ? AND filter_assignment.activo = 1
                     )
                    """);
            args.add(idConductor);
        }
        if (estado != null && !estado.isBlank()) {
            where.append(" AND v.estado = ?");
            args.add(estado.trim());
        } else if (!incluirInactivos) {
            where.append(" AND v.estado = 'activo'");
        }
    }
}

