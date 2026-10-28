package pe.marcona.mtc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.marcona.mtc.dto.ApiResponse;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.AuthService;
import pe.marcona.mtc.service.IConductorService;
import pe.marcona.mtc.service.IEmpresaService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Adaptador REST para operaciones sobre empresas de transporte y conductores.
 * Inyecta las abstracciones segregadas {@link IEmpresaService} e {@link IConductorService} (SRP/DIP).
 */
@RestController
@RequestMapping("/api")
public class CompanyDriverController {

    private final IEmpresaService empresaService;
    private final IConductorService conductorService;
    private final AuthService authService;

    public CompanyDriverController(
            IEmpresaService empresaService,
            IConductorService conductorService,
            AuthService authService) {
        this.empresaService = empresaService;
        this.conductorService = conductorService;
        this.authService = authService;
    }

    @GetMapping("/companies/list.php")
    public ApiResponse<Map<String, Object>> listCompanies(
            @RequestParam Map<String, String> query, HttpServletRequest request) {
        authService.requireUserInRoles(request, Set.of("admin", "operador"));
        return ApiResponse.success(empresaService.listarEmpresas(query), null);
    }

    @GetMapping("/companies/get.php")
    public ApiResponse<Map<String, Object>> getCompany(
            @RequestParam long id, HttpServletRequest request) {
        authService.requireUserInRoles(request, Set.of("admin", "operador"));
        return ApiResponse.success(empresaService.obtenerEmpresa(id), null);
    }

    @PostMapping("/companies/create.php")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Object>> createCompany(
            @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> actor = authService.requireUser(request, "admin");
        long actorId = ((Number) actor.get("id")).longValue();
        return ApiResponse.success(empresaService.crearEmpresa(
                body == null ? Map.of() : body, request, actorId), "Empresa creada exitosamente");
    }

    @PutMapping("/companies/update.php")
    public ApiResponse<Map<String, Object>> updateCompany(
            @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> actor = authService.requireUser(request, "admin");
        long actorId = ((Number) actor.get("id")).longValue();
        return ApiResponse.success(empresaService.actualizarEmpresa(
                body == null ? Map.of() : body, request, actorId), "Empresa actualizada exitosamente");
    }

    @PutMapping("/companies/toggle-status.php")
    public ApiResponse<Map<String, Object>> toggleCompanyStatus(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) String id,
            HttpServletRequest request) {
        Map<String, Object> actor = authService.requireUser(request, "admin");
        long actorId = ((Number) actor.get("id")).longValue();
        Object requestedId = body != null && body.get("id") != null ? body.get("id") : id;
        return ApiResponse.success(empresaService.alternarEstado(parseId(requestedId), request, actorId),
                "Estado actualizado exitosamente");
    }

    @GetMapping("/drivers/list.php")
    public ApiResponse<Map<String, Object>> listDrivers(
            @RequestParam Map<String, String> query, HttpServletRequest request) {
        authService.requireUserInRoles(request, Set.of("admin", "operador"));
        return ApiResponse.success(conductorService.listarConductores(query), null);
    }

    @PostMapping("/drivers/create.php")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Object>> createDriver(
            @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> actor = authService.requireUserInRoles(request, Set.of("admin", "operador"));
        long actorId = ((Number) actor.get("id")).longValue();
        return ApiResponse.success(conductorService.crearConductor(
                body == null ? Map.of() : body, request, actorId), "Chofer creado exitosamente");
    }

    @RequestMapping(value = "/drivers/update.php", method = {RequestMethod.POST, RequestMethod.PUT})
    public ApiResponse<Map<String, Object>> updateDriver(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) String id,
            HttpServletRequest request) {
        Map<String, Object> actor = authService.requireUserInRoles(request, Set.of("admin", "operador"));
        long actorId = ((Number) actor.get("id")).longValue();
        Map<String, Object> input = body == null ? new LinkedHashMap<>() : new LinkedHashMap<>(body);
        if (!input.containsKey("id") && id != null) {
            input.put("id", id);
        }
        return ApiResponse.success(conductorService.actualizarConductor(
                input, request, actorId), "Chofer actualizado exitosamente");
    }

    private long parseId(Object value) {
        if (value == null || value.toString().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ID de empresa requerido");
        }
        try {
            long id = Long.parseLong(value.toString());
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ID de empresa requerido");
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "ID de empresa requerido");
    }
}
