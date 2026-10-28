package pe.marcona.mtc.service.impl;

import com.google.common.base.Preconditions;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import pe.marcona.mtc.dao.IConductorDao;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.IConductorService;
import pe.marcona.mtc.service.IAuditoriaService;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

/**
 * Implementación de la lógica de negocio para la gestión exclusiva de conductores.
 * Aplica Inversión de Dependencias (DIP) inyectando {@link IConductorDao} e {@link IAuditoriaService}.
 */
@Service
public class ConductorServiceImpl implements IConductorService {

    private static final Logger log = LoggerFactory.getLogger(ConductorServiceImpl.class);

    private static final Set<String> ESTADOS_CONDUCTOR = Set.of("activo", "inactivo", "suspendido");

    private final IConductorDao conductorDao;
    private final IAuditoriaService auditoriaService;

    public ConductorServiceImpl(IConductorDao conductorDao, IAuditoriaService auditoriaService) {
        this.conductorDao = Preconditions.checkNotNull(conductorDao, "El DAO de conductor no puede ser nulo");
        this.auditoriaService = Preconditions.checkNotNull(auditoriaService, "El servicio de auditoría no puede ser nulo");
    }

    @Override
    public Map<String, Object> listarConductores(Map<String, String> consulta) {
        int pagina = enteroPositivo(consulta.get("page"), 1);
        int limite = enteroPositivo(consulta.get("limit"), 10);
        String busqueda = texto(consulta.get("search"));
        Long idEmpresa = parsearIdOpcional(consulta.get("company_id"));
        String estado = texto(consulta.get("estado"));
        boolean incluirInactivos = esVerdadero(consulta.get("include_inactive"));

        long totalRegistros = conductorDao.contarConductores(busqueda, idEmpresa, estado, incluirInactivos);
        long desplazamiento = (long) (pagina - 1) * limite;

        List<Map<String, Object>> conductores = conductorDao.listarConductores(
                        busqueda, idEmpresa, estado, incluirInactivos, limite, desplazamiento)
                .stream()
                .map(this::formatearFechas)
                .toList();

        return Map.of(
                "data", conductores,
                "pagination", Map.of(
                        "page", pagina,
                        "limit", limite,
                        "total", totalRegistros,
                        "pages", (totalRegistros + limite - 1) / limite
                )
        );
    }

    @Override
    public Map<String, Object> crearConductor(Map<String, Object> entrada, HttpServletRequest solicitud, long idActor) {
        String nombre = textoRequerido(entrada, "nombre_completo", 3, 150);
        String dni = textoRequerido(entrada, "dni", 8, 8);
        String telefono = textoRequerido(entrada, "telefono", 7, 15);
        String licencia = textoRequerido(entrada, "numero_licencia", 5, 15);
        LocalDate vencimiento = parsearFecha(textoRequerido(entrada, "fecha_vencimiento_licencia", 1, 20));

        if (!dni.matches("\\d{8}")) {
            throw errorValidacion("El DNI debe tener 8 dígitos");
        }
        if (!telefono.matches("\\d{9}")) {
            throw errorValidacion("El teléfono no es válido");
        }
        if (!vencimiento.isAfter(LocalDate.now())) {
            throw errorValidacion("La fecha debe ser posterior a hoy");
        }

        Long idUsuario = parsearIdNulo(entrada.get("user_id"), "user_id");
        Long idEmpresa = parsearIdNulo(entrada.get("company_id"), "company_id");

        if (conductorDao.existePorColumna("dni", dni, null)) {
            throw errorValidacion("El DNI ya está registrado");
        }
        if (conductorDao.existePorColumna("numero_licencia", licencia, null)) {
            throw errorValidacion("El número de licencia ya existe");
        }

        try {
            conductorDao.insertarConductor(idUsuario, idEmpresa, nombre, dni, telefono, licencia, vencimiento);
        } catch (DuplicateKeyException ex) {
            throw errorValidacion("El DNI o número de licencia ya está registrado");
        }

        Map<String, Object> conductor = conductorDao.obtenerPorDni(dni)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chofer no encontrado"));

        long id = ((Number) conductor.get("id")).longValue();
        auditoriaService.registrarAccion(idActor, "crear", "chofer", id, "Chofer creado: " + nombre, solicitud);
        log.info("Conductor creado exitosamente: id={}, dni={}", id, dni);

        return formatearFechas(conductor);
    }

    @Override
    public Map<String, Object> actualizarConductor(Map<String, Object> entrada, HttpServletRequest solicitud, long idActor) {
        long id = parsearIdPositivo(entrada.get("id"));
        conductorDao.obtenerPorId(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chofer no encontrado"));

        Map<String, Object> campos = new LinkedHashMap<>();

        for (String campo : List.of("user_id", "company_id")) {
            if (entrada.containsKey(campo)) {
                campos.put(campo, parsearIdNulo(entrada.get(campo), campo));
            }
        }

        for (String campo : List.of("nombre_completo", "dni", "telefono", "numero_licencia", "fecha_vencimiento_licencia", "estado")) {
            String valor = texto(entrada.get(campo));
            if (valor.isEmpty()) {
                continue;
            }
            validarCampoConductor(campo, valor);
            if (Set.of("dni", "numero_licencia").contains(campo)
                    && conductorDao.existePorColumna(campo, valor, id)) {
                throw errorValidacion("dni".equals(campo) ? "El DNI ya está registrado" : "El número de licencia ya existe");
            }
            if ("fecha_vencimiento_licencia".equals(campo)) {
                campos.put(campo, Date.valueOf(parsearFecha(valor)));
            } else {
                campos.put(campo, valor);
            }
        }

        if (campos.isEmpty()) {
            throw errorValidacion("No hay campos para actualizar");
        }

        try {
            conductorDao.actualizarConductor(id, campos);
        } catch (DuplicateKeyException ex) {
            throw errorValidacion("El DNI o número de licencia ya está registrado");
        }

        auditoriaService.registrarAccion(idActor, "actualizar", "chofer", id, "Chofer actualizado", solicitud);
        log.info("Conductor actualizado: id={}", id);

        return formatearFechas(conductorDao.obtenerPorId(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chofer no encontrado")));
    }

    private void validarCampoConductor(String campo, String valor) {
        switch (campo) {
            case "nombre_completo" -> validarLongitud(valor, 3, 150, campo);
            case "dni" -> {
                if (!valor.matches("\\d{8}")) throw errorValidacion("El DNI debe tener 8 dígitos");
            }
            case "telefono" -> {
                if (!valor.matches("\\d{9}")) throw errorValidacion("El teléfono no es válido");
            }
            case "numero_licencia" -> validarLongitud(valor, 5, 15, campo);
            case "fecha_vencimiento_licencia" -> parsearFecha(valor);
            case "estado" -> {
                if (!ESTADOS_CONDUCTOR.contains(valor)) throw errorValidacion("Estado inválido");
            }
            default -> throw new IllegalArgumentException("Campo de conductor no admitido: " + campo);
        }
    }

    private void validarLongitud(String valor, int min, int max, String campo) {
        if (valor.length() < min || valor.length() > max) {
            throw errorValidacion("Validación fallida: " + campo + " debe tener entre " + min + " y " + max + " caracteres");
        }
    }

    private Map<String, Object> formatearFechas(Map<String, Object> fila) {
        var salida = new LinkedHashMap<String, Object>(fila);
        salida.replaceAll((clave, valor) -> {
            if (valor instanceof Timestamp ts) {
                return ts.toLocalDateTime().toString().replace('T', ' ');
            }
            if (valor instanceof Date d) {
                return d.toLocalDate().toString();
            }
            return valor;
        });
        return salida;
    }

    private String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }

    private String textoRequerido(Map<String, Object> entrada, String campo, int min, int max) {
        String valor = texto(entrada.get(campo));
        validarLongitud(valor, min, max, campo);
        return valor;
    }

    private long parsearIdPositivo(Object valor) {
        if (valor == null || valor.toString().isBlank()) {
            throw errorValidacion("ID inválido");
        }
        try {
            long id = Long.parseLong(valor.toString());
            if (id > 0) return id;
        } catch (NumberFormatException ignored) { }
        throw errorValidacion("ID inválido");
    }

    private Long parsearIdOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            long id = Long.parseLong(valor.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            throw errorValidacion("El identificador debe ser numérico");
        }
    }

    private Long parsearIdNulo(Object valor, String campo) {
        if (valor == null || valor.toString().isBlank()) {
            return null;
        }
        try {
            long id = Long.parseLong(valor.toString().trim());
            if (id > 0) return id;
        } catch (NumberFormatException ignored) { }
        throw errorValidacion("El campo " + campo + " debe ser numérico");
    }

    private int enteroPositivo(String valor, int porDefecto) {
        if (valor == null || valor.isBlank()) return porDefecto;
        try {
            return Math.max(1, Integer.parseInt(valor));
        } catch (NumberFormatException e) {
            throw errorValidacion("Parámetro de paginación inválido");
        }
    }

    private boolean esVerdadero(String valor) {
        return valor != null && !valor.isBlank() && !"0".equals(valor);
    }

    private LocalDate parsearFecha(String valor) {
        try {
            return LocalDate.parse(valor);
        } catch (RuntimeException ex) {
            throw errorValidacion("La fecha fecha_vencimiento_licencia no es válida");
        }
    }

    private ApiException errorValidacion(String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
