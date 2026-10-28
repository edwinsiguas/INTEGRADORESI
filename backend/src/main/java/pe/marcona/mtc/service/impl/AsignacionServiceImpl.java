package pe.marcona.mtc.service.impl;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.marcona.mtc.dao.IAsignacionDao;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.IAsignacionService;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

/**
 * Implementación de la lógica de negocio para la asignación de conductores a vehículos.
 * Aplica Inversión de Dependencias (DIP) inyectando {@link IAsignacionDao}.
 */
@Service
public class AsignacionServiceImpl implements IAsignacionService {

    private static final Logger log = LoggerFactory.getLogger(AsignacionServiceImpl.class);
    private static final Set<String> TIPOS = Set.of("propietario", "alquiler", "reemplazo_temporal");

    private final IAsignacionDao asignacionDao;

    public AsignacionServiceImpl(IAsignacionDao asignacionDao) {
        this.asignacionDao = Preconditions.checkNotNull(asignacionDao, "El DAO de asignaciones no puede ser nulo");
    }

    @Override
    public Map<String, Object> listar(Map<String, String> consulta) {
        int pagina = enteroPositivo(consulta.get("page"), 1);
        int limite = enteroPositivo(consulta.get("limit"), 10);
        long desplazamiento = (long) (pagina - 1) * limite;

        Long idVehiculo = parsearIdOpcional(consulta.get("vehicle_id"));
        Long idConductor = parsearIdOpcional(consulta.get("driver_id"));
        String tipo = texto(consulta.get("tipo"));
        String activoStr = consulta.get("activo");
        Integer activo = (activoStr == null || activoStr.isBlank()) ? 1 : parsearEntero(activoStr, "activo");

        long totalRegistros = asignacionDao.contarAsignaciones(idVehiculo, idConductor, tipo, activo);
        List<Map<String, Object>> datos = asignacionDao.listarAsignaciones(
                        idVehiculo, idConductor, tipo, activo, limite, desplazamiento)
                .stream()
                .map(this::formatearValores)
                .toList();

        return Map.of(
                "data", datos,
                "pagination", Map.of(
                        "page", pagina,
                        "limit", limite,
                        "total", totalRegistros,
                        "pages", totalRegistros == 0 ? 0 : (totalRegistros + limite - 1) / limite
                )
        );
    }

    @Override
    @Transactional
    public Map<String, Object> asignar(Map<String, Object> datosEntrada, long idActor) {
        long idVehiculo = parsearLongPositivo(datosEntrada.get("vehicle_id"), "vehicle_id y driver_id son obligatorios");
        long idConductor = parsearLongPositivo(datosEntrada.get("driver_id"), "vehicle_id y driver_id son obligatorios");
        String tipo = texto(datosEntrada.getOrDefault("tipo", "propietario"));

        if (!TIPOS.contains(tipo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Tipo de asignación inválido. Use: propietario, alquiler, reemplazo_temporal");
        }

        String propietarioNombre = textoOpcional(datosEntrada.get("propietario_nombre"));
        String propietarioDni = textoOpcional(datosEntrada.get("propietario_dni"));
        if ("alquiler".equals(tipo)) {
            if (propietarioNombre == null || propietarioNombre.length() < 3) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "propietario_nombre es obligatorio para tipo alquiler");
            }
            if (propietarioDni == null || !propietarioDni.matches("\\d{8}")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "propietario_dni debe tener 8 dígitos");
            }
        }

        Map<String, Object> vehiculo = asignacionDao.obtenerVehiculoPorId(idVehiculo)
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Vehículo no encontrado o inactivo"));

        if ("inactivo".equals(vehiculo.get("estado"))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Vehículo no encontrado o inactivo");
        }

        Map<String, Object> conductor = asignacionDao.obtenerConductorPorId(idConductor)
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Conductor no encontrado o no activo"));

        if (!"activo".equals(conductor.get("estado"))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Conductor no encontrado o no activo");
        }

        LocalDate vencimiento = convertirALocalDate(conductor.get("fecha_vencimiento_licencia"));
        if (vencimiento.isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La licencia de conducir del conductor está vencida");
        }

        asignacionDao.desactivarAsignacionActivaPorVehiculo(idVehiculo);

        String fechaFin = textoOpcional(datosEntrada.get("fecha_fin"));
        String notas = textoOpcional(datosEntrada.get("notas"));

        long nuevaAsignacionId = asignacionDao.insertarAsignacion(
                idVehiculo, idConductor, tipo, propietarioNombre, propietarioDni, fechaFin, notas, idActor);

        log.info("Conductor id={} asignado a vehículo id={}, asignacionId={}", idConductor, idVehiculo, nuevaAsignacionId);

        return asignacionDao.obtenerPorId(nuevaAsignacionId)
                .map(this::formatearValores)
                .orElseThrow(() -> new IllegalStateException("Asignación " + nuevaAsignacionId + " no encontrada"));
    }

    @Override
    public Map<String, Object> activaPorVehiculo(long idVehiculo) {
        return asignacionDao.obtenerActivaPorVehiculo(idVehiculo)
                .map(this::formatearValores)
                .orElse(null);
    }

    @Override
    public Map<String, Object> activaPorConductor(long idConductor) {
        return asignacionDao.obtenerActivaPorConductor(idConductor)
                .map(this::formatearValores)
                .orElse(null);
    }

    @Override
    public List<Map<String, Object>> historial(long idVehiculo) {
        return asignacionDao.obtenerHistorialPorVehiculo(idVehiculo)
                .stream()
                .map(this::formatearValores)
                .toList();
    }

    @Override
    @Transactional
    public boolean desasignar(long idVehiculo) {
        int filas = asignacionDao.desasignarVehiculo(idVehiculo);
        log.info("Desasignación para vehículo id={}: filas={}", idVehiculo, filas);
        return filas > 0;
    }

    private Map<String, Object> formatearValores(Map<String, Object> fila) {
        var resultado = new LinkedHashMap<String, Object>(fila);
        resultado.replaceAll((clave, valor) -> {
            if (valor instanceof Timestamp timestamp) {
                return timestamp.toLocalDateTime().toString().replace('T', ' ');
            }
            if (valor instanceof Date date) {
                return date.toLocalDate().toString();
            }
            if (valor instanceof Boolean bool && "activo".equals(clave)) {
                return bool ? 1 : 0;
            }
            return valor;
        });
        return resultado;
    }

    private LocalDate convertirALocalDate(Object valor) {
        if (valor instanceof Date date) return date.toLocalDate();
        if (valor instanceof LocalDate date) return date;
        return LocalDate.parse(String.valueOf(valor));
    }

    private int enteroPositivo(String valor, int porDefecto) {
        if (valor == null || valor.isBlank()) return porDefecto;
        int parseado = parsearEntero(valor, "paginación");
        return Math.max(1, parseado);
    }

    private int parsearEntero(String valor, String nombre) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Parámetro " + nombre + " inválido");
        }
    }

    private long parsearLongPositivo(Object valor, String mensaje) {
        long parseado = parsearLong(valor, "id");
        if (parseado < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, mensaje);
        }
        return parseado;
    }

    private long parsearLong(Object valor, String nombre) {
        try {
            return Long.parseLong(String.valueOf(valor));
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Parámetro " + nombre + " inválido");
        }
    }

    private Long parsearIdOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Parámetro id inválido");
        }
    }

    private String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }

    private String textoOpcional(Object valor) {
        String texto = texto(valor);
        return texto.isEmpty() ? null : texto;
    }
}
