package pe.marcona.mtc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.marcona.mtc.dto.ApiResponse;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.IAutenticacionService;
import pe.marcona.mtc.service.IVehiculoConsultaService;
import pe.marcona.mtc.service.IVehiculoMutacionService;

import java.util.Map;
import java.util.Set;

/**
 * Adaptador REST para consultas y operaciones sobre vehículos.
 * Inyecta {@link IVehiculoConsultaService} para lectura estricta, {@link IVehiculoMutacionService} para mutación e {@link IAutenticacionService}.
 */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleQueryController {

    private final IVehiculoConsultaService vehiculoConsultaService;
    private final IVehiculoMutacionService vehiculoMutacionService;
    private final IAutenticacionService autenticacionService;

    public VehicleQueryController(
            IVehiculoConsultaService vehiculoConsultaService,
            IVehiculoMutacionService vehiculoMutacionService,
            IAutenticacionService autenticacionService) {
        this.vehiculoConsultaService = vehiculoConsultaService;
        this.vehiculoMutacionService = vehiculoMutacionService;
        this.autenticacionService = autenticacionService;
    }

    @GetMapping("/list.php")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam Map<String, String> query, HttpServletRequest request) {
        autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        return ApiResponse.success(vehiculoConsultaService.listar(query), null);
    }

    @PutMapping("/toggle-status.php")
    public ApiResponse<Map<String, Object>> toggleStatus(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) String id,
            HttpServletRequest request) {
        Map<String, Object> actor = autenticacionService.requerirUsuario(request, "admin");
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
