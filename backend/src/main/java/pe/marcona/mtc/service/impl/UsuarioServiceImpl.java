package pe.marcona.mtc.service.impl;

import com.google.common.base.Preconditions;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import pe.marcona.mtc.dao.IUsuarioDao;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.IAuditoriaService;
import pe.marcona.mtc.service.IAutenticacionService;
import pe.marcona.mtc.service.IUsuarioService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Implementación de la lógica de negocio para la gestión de usuarios.
 * Aplica Inversión de Dependencias inyectando {@link IUsuarioDao}, {@link IAutenticacionService} e {@link IAuditoriaService}.
 */
@Service
public class UsuarioServiceImpl implements IUsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioServiceImpl.class);

    private static final Set<String> ROLES = Set.of("admin", "operador", "fiscalizador");
    private static final Set<String> ESTADOS = Set.of("activo", "inactivo");
    private static final DateTimeFormatter TIMESTAMP_BD = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IUsuarioDao usuarioDao;
    private final IAutenticacionService autenticacionService;
    private final IAuditoriaService auditoriaService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public UsuarioServiceImpl(
            IUsuarioDao usuarioDao,
            IAutenticacionService autenticacionService,
            IAuditoriaService auditoriaService) {
        this.usuarioDao = Preconditions.checkNotNull(usuarioDao, "El DAO de usuario no puede ser nulo");
        this.autenticacionService = Preconditions.checkNotNull(autenticacionService, "El servicio de autenticación no puede ser nulo");
        this.auditoriaService = Preconditions.checkNotNull(auditoriaService, "El servicio de auditoría no puede ser nulo");
    }

    @Override
    public Map<String, Object> listar(Map<String, String> consulta, HttpServletRequest solicitud) {
        autenticacionService.requerirUsuario(solicitud, "admin");

        int pagina = enteroPositivo(consulta.get("page"), 1);
        int limite = enteroPositivo(consulta.get("limit"), 10);
        String busqueda = limpiarCadena(consulta.get("search"));
        String rol = limpiarCadena(consulta.get("role"));
        String estado = limpiarCadena(consulta.get("estado"));
        boolean incluirInactivos = esVerdadero(consulta.get("include_inactive"));

        long totalRegistros = usuarioDao.contarUsuarios(busqueda, rol, estado, incluirInactivos);
        long desplazamiento = (long) (pagina - 1) * limite;

        List<Map<String, Object>> usuarios = usuarioDao.listarUsuarios(
                        busqueda, rol, estado, incluirInactivos, limite, desplazamiento)
                .stream()
                .map(this::formatearMarcasDeTiempo)
                .toList();

        long totalPaginas = (totalRegistros + limite - 1) / limite;

        return Map.of(
                "data", usuarios,
                "pagination", Map.of(
                        "page", pagina,
                        "limit", limite,
                        "total", totalRegistros,
                        "pages", totalPaginas
                )
        );
    }

    @Override
    public Map<String, Object> crear(Map<String, Object> datosEntrada, HttpServletRequest solicitud) {
        Map<String, Object> actor = autenticacionService.requerirUsuario(solicitud, "admin");

        String email = texto(datosEntrada, "email");
        String password = texto(datosEntrada, "password");
        String nombre = texto(datosEntrada, "nombre");
        String rol = texto(datosEntrada, "role");

        if (email.isBlank() || !validarEmail(email)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Validación fallida: email no es válido");
        }
        if (password.length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Validación fallida: la contraseña debe tener al menos 8 caracteres");
        }
        if (nombre.length() < 3 || nombre.length() > 150) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Validación fallida: nombre debe tener entre 3 y 150 caracteres");
        }
        if (!ROLES.contains(rol)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Validación fallida: rol no es válido");
        }
        if (usuarioDao.existePorEmail(email, null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El email ya está registrado");
        }

        try {
            usuarioDao.insertarUsuario(email, passwordEncoder.encode(password), nombre, rol);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El email ya está registrado");
        }

        Map<String, Object> creado = usuarioDao.obtenerPorEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al recuperar usuario creado"));

        long idUsuario = ((Number) creado.get("id")).longValue();
        long idActor = ((Number) actor.get("id")).longValue();

        auditoriaService.registrarAccion(idActor, "crear", "usuario", idUsuario, "Usuario creado: " + email, solicitud);
        log.info("Usuario creado exitosamente: id={}, email={}", idUsuario, email);

        return formatearMarcasDeTiempo(creado);
    }

    @Override
    public Map<String, Object> actualizar(Map<String, Object> datosEntrada, HttpServletRequest solicitud) {
        Map<String, Object> actor = autenticacionService.requerirUsuario(solicitud, null);
        Long idUsuario = parsearId(datosEntrada == null ? null : datosEntrada.get("id"));
        if (idUsuario == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ID inválido");
        }

        long idActor = ((Number) actor.get("id")).longValue();
        boolean esAdmin = "admin".equals(actor.get("role"));
        if (idActor != idUsuario && !esAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No tienes permiso para editar este usuario");
        }

        if (usuarioDao.obtenerPorId(idUsuario).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }

        Map<String, Object> cambios = new LinkedHashMap<>();

        String email = textoOpcional(datosEntrada, "email");
        if (email != null) {
            if (!validarEmail(email)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Email inválido");
            }
            if (usuarioDao.existePorEmail(email, idUsuario)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El email ya está registrado");
            }
            cambios.put("email", email);
        }

        String nombre = textoOpcional(datosEntrada, "nombre");
        if (nombre != null) {
            cambios.put("nombre", nombre);
        }

        String password = textoOpcional(datosEntrada, "password");
        if (password != null) {
            if (password.length() < 8) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 8 caracteres");
            }
            cambios.put("password", passwordEncoder.encode(password));
        }

        if (esAdmin) {
            String rol = textoOpcional(datosEntrada, "role");
            if (rol != null) {
                if (!ROLES.contains(rol)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Rol inválido");
                }
                cambios.put("role", rol);
            }
            Boolean activo = parsearBooleano(valorOpcional(datosEntrada, "is_active"));
            if (activo != null) {
                cambios.put("is_active", activo ? 1 : 0);
            }
            String estado = textoOpcional(datosEntrada, "estado");
            if (estado != null) {
                if (!ESTADOS.contains(estado)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Estado inválido");
                }
                cambios.put("estado", estado);
            }
        }

        if (cambios.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No hay campos para actualizar");
        }

        try {
            usuarioDao.actualizarCampos(idUsuario, cambios);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El email ya está registrado");
        }

        auditoriaService.registrarAccion(idActor, "actualizar", "usuario", idUsuario, "Credenciales actualizadas", solicitud);
        log.info("Usuario actualizado exitosamente: id={}", idUsuario);

        Map<String, Object> actualizado = usuarioDao.obtenerPorId(idUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al recuperar usuario actualizado"));

        return formatearMarcasDeTiempo(actualizado);
    }

    private Map<String, Object> formatearMarcasDeTiempo(Map<String, Object> fila) {
        var resultado = new LinkedHashMap<String, Object>(fila);
        resultado.replaceAll((clave, valor) -> {
            if (valor instanceof Timestamp marcaTiempo) {
                return marcaTiempo.toLocalDateTime().format(TIMESTAMP_BD);
            }
            if (valor instanceof LocalDateTime fechaHora) {
                return fechaHora.format(TIMESTAMP_BD);
            }
            return valor;
        });
        Object activo = resultado.get("is_active");
        if (activo instanceof Boolean booleano) {
            resultado.put("is_active", booleano ? 1 : 0);
        }
        return resultado;
    }

    private int enteroPositivo(String valor, int valorPorDefecto) {
        try {
            return valor == null ? valorPorDefecto : Math.max(1, Integer.parseInt(valor));
        } catch (NumberFormatException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Parámetro de paginación inválido");
        }
    }

    private boolean esVerdadero(String valor) {
        return valor != null && !valor.isBlank() && !"0".equals(valor);
    }

    private String limpiarCadena(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String texto(Map<String, Object> entrada, String clave) {
        Object valor = entrada == null ? null : entrada.get(clave);
        return valor == null ? "" : valor.toString().trim();
    }

    private String textoOpcional(Map<String, Object> entrada, String clave) {
        Object valor = valorOpcional(entrada, clave);
        if (valor == null) {
            return null;
        }
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private Object valorOpcional(Map<String, Object> entrada, String clave) {
        return entrada == null ? null : entrada.get(clave);
    }

    private Long parsearId(Object valor) {
        if (valor == null) {
            return null;
        }
        try {
            long id = Long.parseLong(valor.toString());
            return id > 0 ? id : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean parsearBooleano(Object valor) {
        if (valor == null || valor.toString().isBlank()) {
            return null;
        }
        if (valor instanceof Boolean b) {
            return b;
        }
        return switch (valor.toString().toLowerCase(Locale.ROOT)) {
            case "1", "true", "yes", "on" -> true;
            case "0", "false", "no", "off" -> false;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "is_active inválido");
        };
    }

    private boolean validarEmail(String email) {
        return email.length() <= 100 && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }
}
