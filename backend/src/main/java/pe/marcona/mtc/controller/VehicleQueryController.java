package pe.marcona.mtc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.marcona.mtc.dto.ApiResponse;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.AuthService;
import pe.marcona.mtc.service.IVehiculoConsultaService;
import pe.marcona.mtc.service.IVehiculoMutacionService;

import java.util.Map;
import java.util.Set;

/**
 * Adaptador REST para consultas y operaciones sobre vehículos.
 * Inyecta {@link IVehiculoConsultaService} para lectura estricta e {@link IVehiculoMutacionService} para cambio de estado.
 */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleQueryController {

    private final IVehiculoConsultaService vehiculoConsultaService;
    private final IVehiculoMutacionService vehiculoMutacionService;
    private final AuthService authService;

    public VehicleQueryController(
            IVehiculoConsultaService vehiculoConsultaService,
            IVehiculoMutacionService vehiculoMutacionService,
            AuthService authService) {
        this.vehiculoConsultaService = vehiculoConsultaService;
        this.vehiculoMutacionService = vehiculoMutacionService;
        this.authService = authService;
    }

    @GetMapping("/list.php")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam Map<String, String> query, HttpServletRequest request) {
        authService.requireUserInRoles(request, Set.of("admin", "operador"));
        return ApiResponse.success(vehiculoConsultaService.listar(query), null);
    }

    @PutMapping("/toggle-status.php")
    public ApiResponse<Map<String, Object>> toggleStatus(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) String id,
            HttpServletRequest request) {
        Map<String, Object> actor = authService.requireUser(request, "admin");
        Object requestedId = body != null && body.get("id") != null ? body.get("id") : id;
        long vehicleId = parseId(requestedId);
        long actorId = ((Number) actor.get("id")).longValue();
        return ApiResponse.success(vehiculoMutacionService.alternarEstado(vehicleId, request, actorId),
                "Estado actualizado exitosamente");
    }

    private long parseId(Object value) {
        if (value == null || value.toString().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ID de vehículo requerido");
        }
        try {
            long id = Long.parseLong(value.toString());
            if (id > 0) return id;
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ID de vehículo requerido");
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "ID de vehículo requerido");
    }
}
