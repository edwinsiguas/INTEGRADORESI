package pe.marcona.mtc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.marcona.mtc.dto.ApiResponse;
import pe.marcona.mtc.service.IAutenticacionService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IAutenticacionService autenticacionService;

    public AuthController(IAutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @PostMapping("/login.php")
    public ApiResponse<Map<String, Object>> login(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        return ApiResponse.success(autenticacionService.iniciarSesion(body, request), "Login exitoso");
    }

    @PostMapping("/logout.php")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        autenticacionService.cerrarSesion(request);
        return ApiResponse.success(null, "Logout exitoso");
    }

    @PostMapping("/verify-token.php")
    public ApiResponse<Map<String, Object>> verifyToken(HttpServletRequest request) {
        return ApiResponse.success(autenticacionService.verificarToken(request), "Token válido");
    }
}
