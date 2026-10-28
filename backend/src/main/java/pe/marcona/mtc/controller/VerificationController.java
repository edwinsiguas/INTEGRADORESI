package pe.marcona.mtc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.marcona.mtc.dto.ApiResponse;
import pe.marcona.mtc.service.IAutenticacionService;
import pe.marcona.mtc.service.IVerificacionService;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/verifications")
public class VerificationController {

    private static final Set<String> ROLES_PERMITIDOS = Set.of("admin", "operador", "fiscalizador");

    private final IVerificacionService verificacionService;
    private final IAutenticacionService autenticacionService;

    public VerificationController(
            IVerificacionService verificacionService,
            IAutenticacionService autenticacionService) {
        this.verificacionService = verificacionService;
        this.autenticacionService = autenticacionService;
    }

    @PostMapping("/create.php")
    public ApiResponse<Map<String, Object>> create(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Map<String, Object> usuario = autenticacionService.requerirUsuarioEnRoles(request, ROLES_PERMITIDOS);
        return ApiResponse.success(
                verificacionService.crear(body == null ? Map.of() : body, request, usuario),
                "Verificación completada");
    }

    @GetMapping("/list.php")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam Map<String, String> query,
            HttpServletRequest request) {
        Map<String, Object> usuario = autenticacionService.requerirUsuarioEnRoles(request, ROLES_PERMITIDOS);
        return ApiResponse.success(verificacionService.listar(query, usuario), "OK");
    }
}
