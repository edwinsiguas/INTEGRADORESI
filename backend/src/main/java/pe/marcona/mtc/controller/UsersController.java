package pe.marcona.mtc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.marcona.mtc.dto.ApiResponse;
import pe.marcona.mtc.service.IUsuarioService;

import java.util.Map;

/**
 * Controlador REST para la administración de usuarios del sistema.
 * Inyecta la interfaz {@link IUsuarioService} cumpliendo con el principio de Inversión de Dependencias (DIP).
 */
@RestController
@RequestMapping("/api/users")
public class UsersController {

    private final IUsuarioService usuarioService;

    public UsersController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/list.php")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam Map<String, String> query,
            HttpServletRequest request) {
        return ApiResponse.success(usuarioService.listar(query, request), null);
    }

    @PostMapping("/create.php")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Object>> create(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        return ApiResponse.success(usuarioService.crear(body, request), "Usuario creado exitosamente");
    }

    @PostMapping("/update.php")
    public ApiResponse<Map<String, Object>> update(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        return ApiResponse.success(usuarioService.actualizar(body, request), "Usuario actualizado exitosamente");
    }
}
