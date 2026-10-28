package pe.marcona.mtc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.marcona.mtc.dto.ApiResponse;
import pe.marcona.mtc.service.IAutenticacionService;
import pe.marcona.mtc.service.IExportacionService;
import pe.marcona.mtc.service.ILimitePeticionesService;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class ExportController {

    private final IExportacionService exportacionService;
    private final IAutenticacionService autenticacionService;
    private final ILimitePeticionesService limitePeticionesService;

    public ExportController(
            IExportacionService exportacionService,
            IAutenticacionService autenticacionService,
            ILimitePeticionesService limitePeticionesService) {
        this.exportacionService = exportacionService;
        this.autenticacionService = autenticacionService;
        this.limitePeticionesService = limitePeticionesService;
    }

    @GetMapping({"/companies/export.php", "/companies/export"})
    public ApiResponse<List<Map<String, Object>>> exportCompanies(
            @RequestParam Map<String, String> query,
            HttpServletRequest request) {
        limitarPeticiones(request);
        autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        return ApiResponse.success(exportacionService.exportarEmpresas(query), "Exportación exitosa");
    }

    @GetMapping({"/drivers/export.php", "/drivers/export"})
    public ApiResponse<List<Map<String, Object>>> exportDrivers(
            @RequestParam Map<String, String> query,
            HttpServletRequest request) {
        limitarPeticiones(request);
        autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        return ApiResponse.success(exportacionService.exportarConductores(query), "Exportación de conductores exitosa");
    }

    @GetMapping({"/vehicles/export.php", "/vehicles/export"})
    public ApiResponse<List<Map<String, Object>>> exportVehicles(
            @RequestParam Map<String, String> query,
            HttpServletRequest request) {
        limitarPeticiones(request);
        autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        return ApiResponse.success(exportacionService.exportarVehiculos(query), "Exportación de vehículos exitosa");
    }

    @GetMapping({"/permits/export.php", "/permits/export"})
    public ApiResponse<List<Map<String, Object>>> exportPermits(
            @RequestParam Map<String, String> query,
            HttpServletRequest request) {
        limitarPeticiones(request);
        autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        return ApiResponse.success(exportacionService.exportarPermisos(query), "Exportación de permisos exitosa");
    }

    private void limitarPeticiones(HttpServletRequest request) {
        limitePeticionesService.verificar("api_general", request.getRemoteAddr(), 120, 60);
    }
}
