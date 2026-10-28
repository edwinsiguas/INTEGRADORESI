package pe.marcona.mtc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.marcona.mtc.dto.ApiResponse;
import pe.marcona.mtc.service.IAutenticacionService;
import pe.marcona.mtc.service.IAuditoriaService;
import pe.marcona.mtc.service.ILimitePeticionesService;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/logs")
public class ActionLogController {

    private final IAuditoriaService auditoriaService;
    private final IAutenticacionService autenticacionService;
    private final ILimitePeticionesService limitePeticionesService;

    public ActionLogController(
            IAuditoriaService auditoriaService,
            IAutenticacionService autenticacionService,
            ILimitePeticionesService limitePeticionesService) {
        this.auditoriaService = auditoriaService;
        this.autenticacionService = autenticacionService;
        this.limitePeticionesService = limitePeticionesService;
    }

    @GetMapping({"/list.php", ""})
    public ApiResponse<Map<String, Object>> list(
            @RequestParam Map<String, String> query,
            HttpServletRequest request) {
        limitePeticionesService.verificar("api_general", request.getRemoteAddr(), 120, 60);
        autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        return ApiResponse.success(auditoriaService.listarHistorial(query), null);
    }
}
