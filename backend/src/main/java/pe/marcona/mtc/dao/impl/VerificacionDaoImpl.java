package pe.marcona.mtc.dao.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import pe.marcona.mtc.dao.IVerificacionDao;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class VerificacionDaoImpl implements IVerificacionDao {

    private final JdbcTemplate jdbcTemplate;

    public VerificacionDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long insertar(
            long idUsuario,
            String tipoBusqueda,
            String consultaBusqueda,
            String resultado,
            String motivosJson,
            String datosVehiculoJson,
            String direccionIp) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conexion -> {
            var ps = conexion.prepareStatement("""
                    INSERT INTO verifications
                        (user_id, search_type, search_query, resultado, motivos, vehicle_data, ip_address)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, idUsuario);
            ps.setString(2, tipoBusqueda);
            ps.setString(3, consultaBusqueda);
            ps.setString(4, resultado);
            ps.setString(5, motivosJson);
            ps.setString(6, datosVehiculoJson);
            ps.setString(7, direccionIp);
            return ps;
        }, keyHolder);

        Number clave = keyHolder.getKey();
        if (clave == null) {
            throw new IllegalStateException("La inserción de verificación no retornó un ID autogenerado");
        }
        return clave.longValue();
    }

    @Override
    public long contarConFiltros(String condicionWhere, List<Object> parametros) {
        Long conteo = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verifications v" + condicionWhere, Long.class, parametros.toArray());
        return conteo == null ? 0L : conteo;
    }

    @Override
    public List<Map<String, Object>> listarConFiltros(
            String condicionWhere, List<Object> parametros, int limite, long offset) {
        List<Object> argsPaginados = new ArrayList<>(parametros);
        argsPaginados.add(limite);
        argsPaginados.add(offset);
        return jdbcTemplate.queryForList("""
                SELECT v.id, v.search_type, v.search_query, v.resultado, v.motivos,
                       v.vehicle_data, v.ip_address, v.created_at,
                       u.nombre AS verificador_nombre, u.role AS verificador_role
                FROM verifications v
                JOIN users u ON v.user_id = u.id
                """ + condicionWhere + " ORDER BY v.created_at DESC LIMIT ? OFFSET ?", argsPaginados.toArray());
    }

    @Override
    public Map<String, Object> obtenerEstadisticasHoy(long idUsuario) {
        return jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total,
                       COALESCE(SUM(resultado = 'autorizado'), 0) AS autorizados,
                       COALESCE(SUM(resultado = 'rechazado'), 0) AS rechazados
                FROM verifications
                WHERE user_id = ? AND DATE(created_at) = CURDATE()
                """, idUsuario);
    }
}
