package pe.marcona.mtc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.marcona.mtc.dto.ApiResponse;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.IAutenticacionService;
import pe.marcona.mtc.service.ICirculacionService;
import pe.marcona.mtc.service.ILimitePeticionesService;
import pe.marcona.mtc.service.IPermisoService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adaptador REST para la consulta, emisión, revocación y renovación de permisos.
 * Inyecta las abstracciones {@link IPermisoService}, {@link ICirculacionService}, {@link IAutenticacionService} e {@link ILimitePeticionesService} (DIP).
 */
@RestController
@RequestMapping("/api/permits")
public class PermitController {

    private final IPermisoService permisoService;
    private final ICirculacionService circulacionService;
    private final IAutenticacionService autenticacionService;
    private final ILimitePeticionesService limitePeticionesService;

    public PermitController(
            IPermisoService permisoService,
            ICirculacionService circulacionService,
            IAutenticacionService autenticacionService,
            ILimitePeticionesService limitePeticionesService) {
        this.permisoService = permisoService;
        this.circulacionService = circulacionService;
        this.autenticacionService = autenticacionService;
        this.limitePeticionesService = limitePeticionesService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam Map<String, String> query,
            HttpServletRequest request) {
        limitePeticionesService.verificar("api_general", request.getRemoteAddr(), 120, 60);
        autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        return ApiResponse.success(permisoService.listarPermisos(query), null);
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(
            @PathVariable long id,
            HttpServletRequest request) {
        limitePeticionesService.verificar("api_general", request.getRemoteAddr(), 120, 60);
        autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        return ApiResponse.success(permisoService.obtenerPermisoPorId(id), null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Object>> generate(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        limitePeticionesService.verificar("api_general", request.getRemoteAddr(), 120, 60);
        Map<String, Object> user = autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        long userId = ((Number) user.get("id")).longValue();
        Map<String, Object> input = body == null ? Map.of() : body;
        return ApiResponse.success(
                permisoService.generarPermiso(input, userId, request),
                "Permiso generado exitosamente");
    }

    @PutMapping("/{id}/revoke")
    public ApiResponse<Void> revoke(
            @PathVariable long id,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        limitePeticionesService.verificar("api_general", request.getRemoteAddr(), 120, 60);
        Map<String, Object> user = autenticacionService.requerirUsuario(request, "admin");
        long userId = ((Number) user.get("id")).longValue();
        String motivo = body == null ? null : (String) body.get("motivo");

        boolean revocado = permisoService.revocarPermiso(id, motivo, userId, request);
        if (!revocado) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Permiso no encontrado o ya no está vigente");
        }
        return ApiResponse.success(null, "Permiso revocado correctamente");
    }

    @PostMapping("/{id}/renew")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Object>> renew(
            @PathVariable long id,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        limitePeticionesService.verificar("api_general", request.getRemoteAddr(), 120, 60);
        Map<String, Object> user = autenticacionService.requerirUsuario(request, "admin");
        long userId = ((Number) user.get("id")).longValue();
        Map<String, Object> input = body == null ? Map.of() : body;

        return ApiResponse.success(
                permisoService.renovarPermiso(id, input, userId, request),
                "Permiso renovado exitosamente");
    }

    @PostMapping("/verify")
    public ApiResponse<Map<String, Object>> verify(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Map<String, String> queryParams,
            HttpServletRequest request) {
        limitePeticionesService.verificar("permit_verify", request.getRemoteAddr(), 60, 60);

        Map<String, Object> payload = new HashMap<>();
        if (queryParams != null) {
            payload.putAll(queryParams);
        }
        if (body != null) {
            payload.putAll(body);
        }

        Map<String, Object> result = circulacionService.validar(payload);
        return ApiResponse.success(result, String.valueOf(result.get("razon")));
    }

    @GetMapping("/vehicle-images/{id}")
    public ApiResponse<List<Map<String, Object>>> vehicleImages(
            @PathVariable long id,
            HttpServletRequest request) {
        limitePeticionesService.verificar("api_general", request.getRemoteAddr(), 120, 60);
        if (id <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "vehicle_id inválido");
        }
        return ApiResponse.success(permisoService.obtenerImagenesVehiculo(id), null);
    }

    @PostMapping("/expire")
    public ApiResponse<Map<String, Object>> markExpired(HttpServletRequest request) {
        limitePeticionesService.verificar("api_general", request.getRemoteAddr(), 120, 60);
        Map<String, Object> user = autenticacionService.requerirUsuario(request, "admin");
        long userId = ((Number) user.get("id")).longValue();

        List<Long> expired = permisoService.marcarExpirados(userId, request);
        int count = expired.size();
        return ApiResponse.success(
                Map.of("marcados_vencidos", count),
                count + " permiso(s) marcados como vencidos");
    }
}
