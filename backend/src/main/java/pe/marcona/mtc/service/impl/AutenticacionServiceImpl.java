package pe.marcona.mtc.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import pe.marcona.mtc.dao.IAutenticacionDao;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.IAutenticacionService;
import pe.marcona.mtc.service.IAuditoriaService;
import pe.marcona.mtc.service.ILimitePeticionesService;
import pe.marcona.mtc.service.ITokenLegadoService;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class AutenticacionServiceImpl implements IAutenticacionService {

    private final IAutenticacionDao autenticacionDao;
    private final ITokenLegadoService tokenLegadoService;
    private final ILimitePeticionesService limitePeticionesService;
    private final IAuditoriaService auditoriaService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public AutenticacionServiceImpl(
            IAutenticacionDao autenticacionDao,
            ITokenLegadoService tokenLegadoService,
            ILimitePeticionesService limitePeticionesService,
            IAuditoriaService auditoriaService) {
        this.autenticacionDao = autenticacionDao;
        this.tokenLegadoService = tokenLegadoService;
        this.limitePeticionesService = limitePeticionesService;
        this.auditoriaService = auditoriaService;
    }

    @Override
    public Map<String, Object> iniciarSesion(Map<String, Object> entrada, HttpServletRequest solicitud) {
        limitePeticionesService.verificar("login", solicitud.getRemoteAddr(), 5, 900);
        String email = texto(entrada, "email");
        String password = texto(entrada, "password");
        if (email.isBlank() || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || password.length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Validación fallida: email o contraseña inválidos");
        }

        Optional<Map<String, Object>> usuarioOpt = autenticacionDao.obtenerUsuarioActivoPorEmail(email);
        if (usuarioOpt.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos");
        }

        Map<String, Object> usuario = usuarioOpt.get();
        if (!passwordEncoder.matches(password, String.valueOf(usuario.get("password")))) {
            long idUsuario = ((Number) usuario.get("id")).longValue();
            auditoriaService.registrarAccion(idUsuario, "login_intento", "usuario", idUsuario,
                    "Intento fallido de login", solicitud);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos");
        }

        long idUsuario = ((Number) usuario.get("id")).longValue();
        String rol = String.valueOf(usuario.get("role"));
        String token = tokenLegadoService.generar(idUsuario, rol);
        auditoriaService.registrarAccion(idUsuario, "login", "usuario", idUsuario, "Login exitoso", solicitud);
        return Map.of(
                "id", idUsuario,
                "email", usuario.get("email"),
                "nombre", usuario.get("nombre"),
                "role", rol,
                "token", token);
    }

    @Override
    public Map<String, Object> cerrarSesion(HttpServletRequest solicitud) {
        Map<String, Object> usuario = requerirUsuario(solicitud, null);
        long idUsuario = ((Number) usuario.get("id")).longValue();
        auditoriaService.registrarAccion(idUsuario, "logout", "usuario", idUsuario, "Logout realizado", solicitud);
        return null;
    }

    @Override
    public Map<String, Object> verificarToken(HttpServletRequest solicitud) {
        Map<String, Object> usuario = requerirUsuario(solicitud, null);
        return Map.of(
                "id", usuario.get("id"),
                "email", usuario.get("email"),
                "role", usuario.get("role"));
    }

    @Override
    public Map<String, Object> requerirUsuario(HttpServletRequest solicitud, String rolRequerido) {
        return requerirUsuarioEnRoles(solicitud, rolRequerido == null ? Set.of() : Set.of(rolRequerido));
    }

    @Override
    public Map<String, Object> requerirUsuarioEnRoles(HttpServletRequest solicitud, Set<String> rolesRequeridos) {
        String authorization = solicitud.getHeader("Authorization");
        String token;
        if (authorization != null) {
            token = authorization.matches("(?i)^Bearer\\s+.+$")
                    ? authorization.replaceFirst("(?i)^Bearer\\s+", "").trim()
                    : null;
        } else {
            token = solicitud.getHeader("X-Auth-Token");
        }
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token no proporcionado");
        }

        Map<String, Object> claims = tokenLegadoService.verificar(token);
        if (claims == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token inválido o expirado");
        }
        Object userIdClaim = claims.get("user_id");
        long idUsuario = userIdClaim instanceof Number number
                ? number.longValue()
                : Long.parseLong((String) userIdClaim);

        Optional<Map<String, Object>> usuarioOpt = autenticacionDao.obtenerUsuarioPorId(idUsuario);
        if (usuarioOpt.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Usuario inválido o inactivo");
        }

        Map<String, Object> usuario = usuarioOpt.get();
        if ("inactivo".equals(usuario.get("estado")) || !esActivo(usuario.get("is_active"))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Usuario inválido o inactivo");
        }
        if (!rolesRequeridos.isEmpty() && !rolesRequeridos.contains(usuario.get("role"))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No tiene permisos para esta acción");
        }
        return usuario;
    }

    private String texto(Map<String, Object> entrada, String clave) {
        Object valor = entrada == null ? null : entrada.get(clave);
        return valor == null ? "" : valor.toString().trim();
    }

    private boolean esActivo(Object valor) {
        if (valor instanceof Boolean bool) {
            return bool;
        }
        return valor instanceof Number number && number.intValue() == 1;
    }
}

