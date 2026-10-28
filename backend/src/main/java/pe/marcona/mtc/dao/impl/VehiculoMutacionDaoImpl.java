package pe.marcona.mtc.dao.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import pe.marcona.mtc.dao.IVehiculoMutacionDao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación de persistencia para operaciones de mutación de vehículos utilizando JdbcTemplate parametrizado.
 */
@Repository
public class VehiculoMutacionDaoImpl implements IVehiculoMutacionDao {

    private final JdbcTemplate jdbcTemplate;

    public VehiculoMutacionDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existePlaca(String placa, Long idExcluido) {
        String sql = idExcluido == null
                ? "SELECT COUNT(*) FROM vehicles WHERE placa = ?"
                : "SELECT COUNT(*) FROM vehicles WHERE placa = ? AND id <> ?";
        Integer conteo = idExcluido == null
                ? jdbcTemplate.queryForObject(sql, Integer.class, placa)
                : jdbcTemplate.queryForObject(sql, Integer.class, placa, idExcluido);
        return conteo != null && conteo > 0;
    }

    @Override
    public long insertarVehiculo(Long idEmpresa, String tipoPropiedad, String tipoServicio, String categoria,
                                 String placa, String marca, String modelo, String color, Integer anioFabricacion,
                                 String numeroVin, String numeroMotor, String combustible, String soatPoliza,
                                 String soatAseguradora, LocalDate soatVencimiento, String rtEntidad,
                                 String rtCertificado, LocalDate rtVencimiento) {
        String sql = """
                INSERT INTO vehicles (
                    company_id, tipo_propiedad, tipo_servicio, categoria, placa, marca, modelo, color,
                    ano_fabricacion, numero_vin, numero_motor, combustible,
                    soat_poliza, soat_aseguradora, soat_vencimiento,
                    rt_entidad, rt_certificado, rt_vencimiento, estado
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'activo')
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (idEmpresa != null) ps.setLong(1, idEmpresa); else ps.setNull(1, java.sql.Types.BIGINT);
            ps.setString(2, tipoPropiedad);
            ps.setString(3, tipoServicio);
            ps.setString(4, categoria);
            ps.setString(5, placa);
            ps.setString(6, marca);
            ps.setString(7, modelo);
            ps.setString(8, color);
            if (anioFabricacion != null) ps.setInt(9, anioFabricacion); else ps.setNull(9, java.sql.Types.INTEGER);
            ps.setString(10, numeroVin);
            ps.setString(11, numeroMotor);
            ps.setString(12, combustible);
            ps.setString(13, soatPoliza);
            ps.setString(14, soatAseguradora);
            if (soatVencimiento != null) ps.setDate(15, Date.valueOf(soatVencimiento)); else ps.setNull(15, java.sql.Types.DATE);
            ps.setString(16, rtEntidad);
            ps.setString(17, rtCertificado);
            if (rtVencimiento != null) ps.setDate(18, Date.valueOf(rtVencimiento)); else ps.setNull(18, java.sql.Types.DATE);
            return ps;
        }, keyHolder);

        Number claveGenerada = keyHolder.getKey();
        if (claveGenerada == null) {
            throw new IllegalStateException("No se pudo obtener el ID del vehículo registrado");
        }
        return claveGenerada.longValue();
    }

    @Override
    public Optional<Map<String, Object>> obtenerPorId(long id) {
        String sql = "SELECT * FROM vehicles WHERE id = ?";
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(sql, id);
        return filas.isEmpty() ? Optional.empty() : Optional.of(filas.get(0));
    }

    @Override
    public int actualizarVehiculo(long id, List<String> clausulas, List<Object> parametros) {
        List<Object> args = new ArrayList<>(parametros);
        args.add(id);
        String sql = "UPDATE vehicles SET " + String.join(", ", clausulas) + " WHERE id = ?";
        return jdbcTemplate.update(sql, args.toArray());
    }

    @Override
    public int alternarEstado(long id, String nuevoEstado) {
        String sql = "UPDATE vehicles SET estado = ? WHERE id = ?";
        return jdbcTemplate.update(sql, nuevoEstado, id);
    }

    @Override
    public int contarImagenes(long idVehiculo) {
        String sql = "SELECT COUNT(*) FROM vehicle_images WHERE vehicle_id = ?";
        Integer conteo = jdbcTemplate.queryForObject(sql, Integer.class, idVehiculo);
        return conteo == null ? 0 : conteo;
    }

    @Override
    public void insertarImagen(long idVehiculo, String nombreOriginal, String rutaRelativa, long tamanio, int orden) {
        String sql = """
                INSERT INTO vehicle_images (vehicle_id, file_name, file_path, mime_type, file_size, sort_order)
                VALUES (?, ?, ?, 'image/jpeg', ?, ?)
                """;
        jdbcTemplate.update(sql, idVehiculo, nombreOriginal, rutaRelativa, tamanio, orden);
    }
}
