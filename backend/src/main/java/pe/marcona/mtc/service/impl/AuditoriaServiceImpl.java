package pe.marcona.mtc.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import pe.marcona.mtc.dao.IAuditoriaDao;
import pe.marcona.mtc.service.IAuditoriaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de la lógica de negocio para auditoría y registro de acciones.
 * Aplica Inversión de Dependencias (DIP) inyectando {@link IAuditoriaDao}.
 */
@Service
public class AuditoriaServiceImpl implements IAuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaServiceImpl.class);

    private final IAuditoriaDao auditoriaDao;
    private final ObjectMapper objectMapper;
    private final Path archivoAuditoria;

    public AuditoriaServiceImpl(
            IAuditoriaDao auditoriaDao,
            ObjectMapper objectMapper,
            @Value("${app.audit-file:logs/database-audit.jsonl}") String auditFile) {
        this.auditoriaDao = Preconditions.checkNotNull(auditoriaDao, "El DAO de auditoría no puede ser nulo");
        this.objectMapper = Preconditions.checkNotNull(objectMapper, "El mapeador de objetos JSON no puede ser nulo");
        this.archivoAuditoria = Path.of(auditFile).toAbsolutePath().normalize();
    }

    @Override
    public void registrarAccion(
            long idActor,
            String accion,
            String entidad,
            long idEntidad,
            String descripcion,
            HttpServletRequest solicitud) {

        String ip = solicitud != null ? solicitud.getRemoteAddr() : "127.0.0.1";
        try {
            auditoriaDao.insertarRegistro(idActor, accion, entidad, idEntidad, descripcion, ip);
        } catch (DataAccessException ex) {
            log.error("No se pudo insertar el evento de auditoría [{}] en la base de datos", accion, ex);
        }

        var evento = new LinkedHashMap<String, Object>();
        evento.put("timestamp", OffsetDateTime.now().toString());
        evento.put("user_id", idActor);
        evento.put("action", accion);
        evento.put("entity", entidad);
        evento.put("entity_id", idEntidad);
        evento.put("description", descripcion);
        evento.put("ip_address", ip);
        if (solicitud != null) {
            evento.put("http_method", solicitud.getMethod());
            evento.put("endpoint", solicitud.getRequestURI());
        }

        try {
            String linea = objectMapper.writeValueAsString(evento) + System.lineSeparator();
            if (archivoAuditoria.getParent() != null) {
                Files.createDirectories(archivoAuditoria.getParent());
            }
            Files.writeString(archivoAuditoria, linea, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            log.error("No se pudo escribir el evento de auditoría en el archivo [{}]", archivoAuditoria, ex);
        }
    }

    @Override
    public Map<String, Object> listarHistorial(Map<String, String> consulta) {
        int pagina = enteroPositivo(consulta.get("page"), 1);
        int limite = enteroPositivo(consulta.get("limit"), 10);
        long desplazamiento = (long) (pagina - 1) * limite;

        Long idUsuario = parsearLongOpcional(consulta.get("user_id"));
        String accion = texto(consulta.get("accion"));
        String entidad = texto(consulta.get("entidad"));

        long total = auditoriaDao.contarHistorial(idUsuario, accion, entidad);
        List<Map<String, Object>> filas = auditoriaDao.listarHistorial(idUsuario, accion, entidad, limite, desplazamiento)
                .stream()
                .map(this::formatearFechas)
                .toList();

        long paginas = total == 0 ? 0 : (total + limite - 1) / limite;

        return Map.of(
                "data", filas,
                "pagination", Map.of(
                        "page", pagina,
                        "limit", limite,
                        "total", total,
                        "pages", paginas
                )
        );
    }

    private Map<String, Object> formatearFechas(Map<String, Object> fila) {
        var salida = new LinkedHashMap<String, Object>(fila);
        Object creadoEn = salida.get("created_at");
        if (creadoEn instanceof Timestamp ts) {
            salida.put("created_at", ts.toLocalDateTime().toString().replace('T', ' '));
        }
        return salida;
    }

    private int enteroPositivo(String valor, int porDefecto) {
        if (valor == null || valor.isBlank()) return porDefecto;
        try {
            return Math.max(1, Integer.parseInt(valor.trim()));
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    private Long parsearLongOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String texto(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}

