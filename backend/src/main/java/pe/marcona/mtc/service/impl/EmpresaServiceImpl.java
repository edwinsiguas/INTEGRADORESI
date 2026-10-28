package pe.marcona.mtc.service.impl;

import com.google.common.base.Preconditions;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import pe.marcona.mtc.dao.IEmpresaDao;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.AuditService;
import pe.marcona.mtc.service.IEmpresaService;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;

/**
 * Implementación de la lógica de negocio para la gestión exclusiva de empresas.
 * Aplica Inversión de Dependencias (DIP) inyectando {@link IEmpresaDao}.
 */
@Service
public class EmpresaServiceImpl implements IEmpresaService {

    private static final Logger log = LoggerFactory.getLogger(EmpresaServiceImpl.class);

    private final IEmpresaDao empresaDao;
    private final AuditService auditService;

    public EmpresaServiceImpl(IEmpresaDao empresaDao, AuditService auditService) {
        this.empresaDao = Preconditions.checkNotNull(empresaDao, "El DAO de empresa no puede ser nulo");
        this.auditService = Preconditions.checkNotNull(auditService, "El servicio de auditoría no puede ser nulo");
    }

    @Override
    public Map<String, Object> listarEmpresas(Map<String, String> consulta) {
        int pagina = enteroPositivo(consulta.get("page"), 1);
        int limite = enteroPositivo(consulta.get("limit"), 10);
        String busqueda = texto(consulta.get("search"));
        String estado = texto(consulta.get("estado"));

        long totalRegistros = empresaDao.contarEmpresas(busqueda, estado);
        long activasTotal = empresaDao.contarEmpresasActivas();
        long desplazamiento = (long) (pagina - 1) * limite;

        List<Map<String, Object>> empresas = empresaDao.listarEmpresas(busqueda, estado, limite, desplazamiento)
                .stream()
                .map(this::formatearFechas)
                .toList();

        return Map.of(
                "data", empresas,
                "pagination", Map.of(
                        "page", pagina,
                        "limit", limite,
                        "total", totalRegistros,
                        "active_total", activasTotal,
                        "pages", (totalRegistros + limite - 1) / limite
                )
        );
    }

    @Override
    public Map<String, Object> obtenerEmpresa(long id) {
        return empresaDao.obtenerPorId(id)
                .map(this::formatearFechas)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
    }

    @Override
    public Map<String, Object> crearEmpresa(Map<String, Object> entrada, HttpServletRequest solicitud, long idActor) {
        String nombre = textoRequerido(entrada, "nombre", 3, 150);
        String ruc = textoRequerido(entrada, "ruc", 11, 11);
        String telefono = textoRequerido(entrada, "telefono", 7, 15);
        String email = textoRequerido(entrada, "email", 3, 100);
        String direccion = textoRequerido(entrada, "direccion", 5, 255);
        String contacto = textoRequerido(entrada, "contacto", 3, 150);

        if (!ruc.matches("(10|15|17|20)\\d{9}")) {
            throw errorValidacion("El RUC no es válido");
        }
        if (!telefono.matches("\\d{9}")) {
            throw errorValidacion("El teléfono no es válido");
        }
        if (!validarEmail(email)) {
            throw errorValidacion("El email no es válido");
        }
        if (empresaDao.existePorColumna("nombre", nombre, null)) {
            throw errorValidacion("El nombre de la empresa ya existe");
        }
        if (empresaDao.existePorColumna("ruc", ruc, null)) {
            throw errorValidacion("El RUC ya está registrado");
        }

        try {
            empresaDao.insertarEmpresa(nombre, ruc, telefono, email, direccion, contacto);
        } catch (DuplicateKeyException ex) {
            throw errorValidacion("El nombre de la empresa o RUC ya está registrado");
        }

        Map<String, Object> empresa = empresaDao.obtenerPorRuc(ruc)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        long id = ((Number) empresa.get("id")).longValue();
        auditService.record(idActor, "crear", "empresa", id, "Empresa creada: " + nombre, solicitud);
        log.info("Empresa creada con éxito: id={}, ruc={}", id, ruc);

        return formatearFechas(empresa);
    }

    @Override
    public Map<String, Object> actualizarEmpresa(Map<String, Object> entrada, HttpServletRequest solicitud, long idActor) {
        long id = parsearIdPositivo(entrada.get("id"));
        empresaDao.obtenerPorId(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        String nombre = textoRequerido(entrada, "nombre", 3, 150);
        String ruc = textoRequerido(entrada, "ruc", 11, 11);
        String telefono = textoRequerido(entrada, "telefono", 7, 15);
        String email = textoRequerido(entrada, "email", 3, 100);
        String direccion = textoRequerido(entrada, "direccion", 5, 255);
        String contacto = textoRequerido(entrada, "contacto", 3, 150);
        String estado = textoRequerido(entrada, "estado", 1, 20);

        if (!ruc.matches("(10|15|17|20)\\d{9}")) {
            throw errorValidacion("El RUC no es válido");
        }
        if (!telefono.matches("\\d{9}")) {
            throw errorValidacion("El teléfono no es válido");
        }
        if (!validarEmail(email)) {
            throw errorValidacion("El email no es válido");
        }
        if (!Set.of("activo", "inactivo").contains(estado)) {
            throw errorValidacion("El valor de estado no es válido");
        }
        if (empresaDao.existePorColumna("nombre", nombre, id)) {
            throw errorValidacion("El nombre de la empresa ya existe");
        }
        if (empresaDao.existePorColumna("ruc", ruc, id)) {
            throw errorValidacion("El RUC ya está registrado");
        }

        try {
            empresaDao.actualizarEmpresa(id, nombre, ruc, telefono, email, direccion, contacto, estado);
        } catch (DuplicateKeyException ex) {
            throw errorValidacion("El nombre de la empresa o RUC ya está registrado");
        }

        auditService.record(idActor, "actualizar", "empresa", id, "Empresa actualizada: " + nombre, solicitud);
        log.info("Empresa actualizada: id={}", id);

        return formatearFechas(empresaDao.obtenerPorId(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Empresa no encontrada")));
    }

    @Override
    public Map<String, Object> alternarEstado(long id, HttpServletRequest solicitud, long idActor) {
        Map<String, Object> empresa = empresaDao.obtenerPorId(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        String nuevoEstado = "activo".equals(empresa.get("estado")) ? "inactivo" : "activo";
        empresaDao.actualizarEstado(id, nuevoEstado);

        auditService.record(idActor, "cambio_estado", "empresa", id,
                "Estado de empresa '" + empresa.get("nombre") + "' cambiado a " + nuevoEstado, solicitud);
        log.info("Estado de empresa alternado: id={}, estado={}", id, nuevoEstado);

        return Map.of("id", id, "estado", nuevoEstado);
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
        if (valor.length() < min || valor.length() > max) {
            throw errorValidacion("Validación fallida: " + campo + " debe tener entre " + min + " y " + max + " caracteres");
        }
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

    private int enteroPositivo(String valor, int porDefecto) {
        if (valor == null || valor.isBlank()) return porDefecto;
        try {
            return Math.max(1, Integer.parseInt(valor));
        } catch (NumberFormatException e) {
            throw errorValidacion("Parámetro de paginación inválido");
        }
    }

    private boolean validarEmail(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private ApiException errorValidacion(String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
