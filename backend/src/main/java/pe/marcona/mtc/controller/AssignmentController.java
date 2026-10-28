package pe.marcona.mtc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.marcona.mtc.dto.ApiResponse;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.IAsignacionService;
import pe.marcona.mtc.service.IAuditoriaService;
import pe.marcona.mtc.service.IAutenticacionService;
import pe.marcona.mtc.service.ICirculacionService;
import pe.marcona.mtc.service.ILimitePeticionesService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adaptador REST para la gestión y asignación de vehículos a conductores.
 * Inyecta {@link IAsignacionService}, {@link ICirculacionService}, {@link IAutenticacionService}, {@link IAuditoriaService} e {@link ILimitePeticionesService} (DIP).
 */
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final IAsignacionService asignacionService;
    private final ICirculacionService circulacionService;
    private final IAutenticacionService autenticacionService;
    private final IAuditoriaService auditoriaService;
    private final ILimitePeticionesService limitePeticionesService;

    public AssignmentController(
            IAsignacionService asignacionService,
            ICirculacionService circulacionService,
            IAutenticacionService autenticacionService,
            IAuditoriaService auditoriaService,
            ILimitePeticionesService limitePeticionesService) {
        this.asignacionService = asignacionService;
        this.circulacionService = circulacionService;
        this.autenticacionService = autenticacionService;
        this.auditoriaService = auditoriaService;
        this.limitePeticionesService = limitePeticionesService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam Map<String, String> query,
            HttpServletRequest request) {
        limitarPeticionesGenerales(request);
        autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        return ApiResponse.success(asignacionService.listar(query), null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Object>> assign(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        limitarPeticionesGenerales(request);
        Map<String, Object> actor = autenticacionService.requerirUsuario(request, "admin");
        long actorId = ((Number) actor.get("id")).longValue();
        Map<String, Object> assignment = asignacionService.asignar(body == null ? Map.of() : body, actorId);
        long vehicleId = ((Number) assignment.get("vehicle_id")).longValue();
        auditoriaService.registrarAccion(actorId, "asignar_conductor", "vehiculo", vehicleId,
                "Conductor ID " + assignment.get("driver_id") + " asignado como " + assignment.get("tipo"), request);
        return ApiResponse.success(assignment, "Conductor asignado exitosamente");
    }

    @GetMapping("/vehicle/{id}")
    public ApiResponse<Map<String, Object>> byVehicle(@PathVariable long id, HttpServletRequest request) {
        limitarPeticionesGenerales(request);
        autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        Map<String, Object> assignment = asignacionService.activaPorVehiculo(id);
        if (assignment == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No hay conductor asignado a este vehículo");
        }
        return ApiResponse.success(assignment, null);
    }

    @GetMapping("/driver/{id}")
    public ApiResponse<Map<String, Object>> byDriver(@PathVariable long id, HttpServletRequest request) {
        limitarPeticionesGenerales(request);
        autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        Map<String, Object> vehicle = asignacionService.activaPorConductor(id);
        if (vehicle == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "El conductor no tiene vehículo asignado actualmente");
        }
        return ApiResponse.success(vehicle, null);
    }

    @GetMapping("/history/vehicle/{id}")
    public ApiResponse<List<Map<String, Object>>> history(@PathVariable long id, HttpServletRequest request) {
        limitarPeticionesGenerales(request);
        autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        return ApiResponse.success(asignacionService.historial(id), null);
    }

    @DeleteMapping("/vehicle/{id}")
    public ApiResponse<Void> unassign(@PathVariable long id, HttpServletRequest request) {
        limitarPeticionesGenerales(request);
        Map<String, Object> actor = autenticacionService.requerirUsuario(request, "admin");
        long actorId = ((Number) actor.get("id")).longValue();

        boolean unassigned = asignacionService.desasignar(id);
        if (!unassigned) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No se encontró asignación activa para este vehículo");
        }

        auditoriaService.registrarAccion(actorId, "desasignar_conductor", "vehiculo", id,
                "Conductor desasignado del vehículo", request);

        return ApiResponse.success(null, "Conductor desasignado correctamente");
    }

    @PostMapping("/verify")
    public ApiResponse<Map<String, Object>> verify(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Map<String, String> queryParams,
            HttpServletRequest request) {
        limitePeticionesService.verificar("assignment_verify", request.getRemoteAddr(), 60, 60);

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

    private void limitarPeticionesGenerales(HttpServletRequest request) {
        limitePeticionesService.verificar("api_general", request.getRemoteAddr(), 120, 60);
    }
}
