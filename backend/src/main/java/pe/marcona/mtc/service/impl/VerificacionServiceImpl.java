package pe.marcona.mtc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.marcona.mtc.dao.IVerificacionDao;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.IAuditoriaService;
import pe.marcona.mtc.service.ICirculacionService;
import pe.marcona.mtc.service.ILimitePeticionesService;
import pe.marcona.mtc.service.IVerificacionService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class VerificacionServiceImpl implements IVerificacionService {

    private static final Set<String> TIPOS_BUSQUEDA = Set.of("placa", "dni", "permiso");

    private final IVerificacionDao verificacionDao;
    private final ICirculacionService circulacionService;
    private final ILimitePeticionesService limitePeticionesService;
    private final IAuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;

    public VerificacionServiceImpl(
            IVerificacionDao verificacionDao,
            ICirculacionService circulacionService,
            ILimitePeticionesService limitePeticionesService,
            IAuditoriaService auditoriaService,
            ObjectMapper objectMapper) {
        this.verificacionDao = verificacionDao;
        this.circulacionService = circulacionService;
        this.limitePeticionesService = limitePeticionesService;
        this.auditoriaService = auditoriaService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Map<String, Object> crear(
            Map<String, Object> entrada, HttpServletRequest solicitud, Map<String, Object> usuario) {
        limitePeticionesService.verificar("permit_verify", solicitud.getRemoteAddr(), 60, 60);
        String tipo = texto(entrada, "search_type");
        String termino = texto(entrada, "search_query").toUpperCase();
        if (!TIPOS_BUSQUEDA.contains(tipo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Tipo de búsqueda inválido. Use: placa, dni o permiso");
        }
        if (termino.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El término de búsqueda es requerido");
        }

        String claveConsulta = switch (tipo) {
            case "placa" -> "placa";
            case "dni" -> "dni";
            default -> "numero_permiso";
        };
        Map<String, Object> resultado = circulacionService.validar(Map.of(claveConsulta, termino));
        boolean permitido = Boolean.TRUE.equals(resultado.get("puede_circular"));
        Object datos = resultado.get("data");
        List<?> motivos = resultado.get("bloqueos") instanceof List<?> list ? list : List.of();
        long idUsuario = ((Number) usuario.get("id")).longValue();
        String ip = obtenerIpCliente(solicitud);

        long idVerificacion = verificacionDao.insertar(
                idUsuario,
                tipo,
                termino,
                permitido ? "autorizado" : "rechazado",
                aJson(motivos),
                datos == null ? null : aJson(datos),
                ip);

        auditoriaService.registrarAccion(
                idUsuario,
                "registrar_verificacion",
                "verificacion",
                idVerificacion,
                "Verificación de " + tipo + ": " + (permitido ? "autorizado" : "rechazado"),
                solicitud);

        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listar(Map<String, String> consulta, Map<String, Object> usuario) {
        int pagina = parsearEntero(consulta.get("page"), 1);
        int limite = Math.min(50, parsearEntero(consulta.get("limit"), 20));
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        boolean soloMios = "fiscalizador".equals(usuario.get("role"));
        if (soloMios) {
            where.append(" AND v.user_id = ?");
            args.add(usuario.get("id"));
        } else if (consulta.get("user_id") != null && !consulta.get("user_id").isBlank()) {
            where.append(" AND v.user_id = ?");
            args.add(parsearId(consulta.get("user_id")));
        }
        String resultado = consulta.get("resultado");
        if (resultado != null && Set.of("autorizado", "rechazado").contains(resultado)) {
            where.append(" AND v.resultado = ?");
            args.add(resultado);
        }
        agregarFiltroFecha(where, args, consulta.get("date_from"), ">=");
        agregarFiltroFecha(where, args, consulta.get("date_to"), "<=");

        long totalFilas = verificacionDao.contarConFiltros(where.toString(), args);
        List<Map<String, Object>> filas = verificacionDao.listarConFiltros(
                where.toString(), args, limite, (long) (pagina - 1) * limite);
        List<Map<String, Object>> datos = filas.stream().map(this::formatearFila).toList();

        int paginas = totalFilas == 0 ? 1 : (int) ((totalFilas + limite - 1) / limite);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("data", datos);
        respuesta.put("today_stats", soloMios ? verificacionDao.obtenerEstadisticasHoy(((Number) usuario.get("id")).longValue()) : null);
        respuesta.put("pagination", Map.of(
                "page", pagina, "limit", limite, "total", totalFilas, "pages", paginas));
        return respuesta;
    }

    private Map<String, Object> formatearFila(Map<String, Object> fila) {
        var salida = new LinkedHashMap<String, Object>(fila);
        salida.put("motivos", parsearJson(fila.get("motivos"), List.of()));
        salida.put("vehicle_data", parsearJson(fila.get("vehicle_data"), null));
        Object createdAt = fila.get("created_at");
        if (createdAt instanceof java.sql.Timestamp timestamp) {
            salida.put("created_at", timestamp.toLocalDateTime().toString().replace('T', ' '));
        }
        return salida;
    }

    private Object parsearJson(Object valor, Object porDefecto) {
        if (valor == null) return porDefecto;
        try {
            return objectMapper.readValue(valor.toString(), Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("El formato JSON almacenado en la verificación es inválido", e);
        }
    }

    private String aJson(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error al serializar datos de verificación a JSON", e);
        }
    }

    private void agregarFiltroFecha(StringBuilder where, List<Object> args, String fecha, String operador) {
        if (fecha == null || fecha.isBlank()) return;
        try {
            java.time.LocalDate.parse(fecha);
        } catch (java.time.format.DateTimeParseException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Fecha de filtro no válida");
        }
        where.append(" AND DATE(v.created_at) ").append(operador).append(" ?");
        args.add(fecha);
    }

    private int parsearEntero(String valor, int porDefecto) {
        if (valor == null || valor.isBlank()) return porDefecto;
        try {
            return Math.max(1, Integer.parseInt(valor));
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Parámetro de paginación inválido");
        }
    }

    private long parsearId(String valor) {
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El usuario debe ser numérico");
        }
    }

    private String obtenerIpCliente(HttpServletRequest solicitud) {
        for (String encabezado : List.of("CF-Connecting-IP", "X-Real-IP", "X-Forwarded-For")) {
            String valor = solicitud.getHeader(encabezado);
            if (valor != null && !valor.isBlank()) {
                return valor.split(",", 2)[0].trim();
            }
        }
        return solicitud.getRemoteAddr() == null ? "0.0.0.0" : solicitud.getRemoteAddr();
    }

    private String texto(Map<String, Object> entrada, String clave) {
        Object valor = entrada.get(clave);
        return valor == null ? "" : valor.toString().trim();
    }
}

