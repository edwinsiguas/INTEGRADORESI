package pe.marcona.mtc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.marcona.mtc.dto.ApiResponse;
import pe.marcona.mtc.service.IAutenticacionService;
import pe.marcona.mtc.service.IEstadisticasService;
import pe.marcona.mtc.service.ILimitePeticionesService;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/stats")
public class StatisticsController {

    private final IEstadisticasService estadisticasService;
    private final IAutenticacionService autenticacionService;
    private final ILimitePeticionesService limitePeticionesService;

    public StatisticsController(
            IEstadisticasService estadisticasService,
            IAutenticacionService autenticacionService,
            ILimitePeticionesService limitePeticionesService) {
        this.estadisticasService = estadisticasService;
        this.autenticacionService = autenticacionService;
        this.limitePeticionesService = limitePeticionesService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(HttpServletRequest request) {
        limitarPeticionesGenerales(request);
        return ApiResponse.success(estadisticasService.obtenerDashboard(), null);
    }

    @GetMapping("/companies")
    public ApiResponse<Map<String, Object>> companies(HttpServletRequest request) {
        limitarPeticionesGenerales(request);
        return ApiResponse.success(estadisticasService.obtenerEmpresas(), null);
    }

    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> analytics(HttpServletRequest request) {
        limitarPeticionesGenerales(request);
        autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        return ApiResponse.success(estadisticasService.obtenerAnalitica(), "Estadísticas recuperadas");
    }

    private void limitarPeticionesGenerales(HttpServletRequest request) {
        limitePeticionesService.verificar("api_general", request.getRemoteAddr(), 120, 60);
    }
}
