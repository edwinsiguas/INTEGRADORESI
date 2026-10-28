package pe.marcona.mtc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import pe.marcona.mtc.dto.ApiResponse;
import pe.marcona.mtc.service.IAutenticacionService;
import pe.marcona.mtc.service.ILimitePeticionesService;
import pe.marcona.mtc.service.IVehiculoMutacionService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adaptador REST para mutaciones de vehículos (creación, edición y carga de imágenes).
 * Inyecta las interfaces {@link IVehiculoMutacionService}, {@link IAutenticacionService} e {@link ILimitePeticionesService} cumpliendo DIP.
 */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleMutationController {

    private final IVehiculoMutacionService vehiculoMutacionService;
    private final IAutenticacionService autenticacionService;
    private final ILimitePeticionesService limitePeticionesService;

    public VehicleMutationController(
            IVehiculoMutacionService vehiculoMutacionService,
            IAutenticacionService autenticacionService,
            ILimitePeticionesService limitePeticionesService) {
        this.vehiculoMutacionService = vehiculoMutacionService;
        this.autenticacionService = autenticacionService;
        this.limitePeticionesService = limitePeticionesService;
    }

    @PostMapping(value = {"/create.php", ""})
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Object>> create(
            @RequestParam(required = false) Map<String, String> queryParams,
            HttpServletRequest request) {

        limitePeticionesService.verificar("api_general", request.getRemoteAddr(), 120, 60);
        Map<String, Object> user = autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        long actorId = ((Number) user.get("id")).longValue();

        Map<String, String> params = new HashMap<>();
        if (queryParams != null) {
            params.putAll(queryParams);
        }
        request.getParameterMap().forEach((k, v) -> {
            if (v != null && v.length > 0 && !params.containsKey(k)) {
                params.put(k, v[0]);
            }
        });

        List<MultipartFile> files = extractImages(request);

        Map<String, Object> created = vehiculoMutacionService.crearVehiculo(params, files, actorId, request);
        return ApiResponse.success(created, "Vehículo registrado exitosamente");
    }

    @RequestMapping(value = {"/update.php", ""}, method = {RequestMethod.POST, RequestMethod.PUT})
    public ApiResponse<Map<String, Object>> update(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Map<String, String> queryParams,
            HttpServletRequest request) {

        limitePeticionesService.verificar("api_general", request.getRemoteAddr(), 120, 60);
        Map<String, Object> user = autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        long actorId = ((Number) user.get("id")).longValue();

        Map<String, Object> input = new HashMap<>();
        if (queryParams != null) {
            input.putAll(queryParams);
        }
        if (body != null) {
            input.putAll(body);
        }

        Map<String, Object> updated = vehiculoMutacionService.actualizarVehiculo(input, actorId, request);
        return ApiResponse.success(updated, "Vehículo actualizado exitosamente");
    }

    @PostMapping(value = {"/upload-images.php", "/upload-images"})
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Object>> uploadImages(
            @RequestParam("vehicle_id") long vehicleId,
            HttpServletRequest request) {

        limitePeticionesService.verificar("api_general", request.getRemoteAddr(), 120, 60);
        Map<String, Object> user = autenticacionService.requerirUsuarioEnRoles(request, Set.of("admin", "operador"));
        long actorId = ((Number) user.get("id")).longValue();

        List<MultipartFile> files = extractImages(request);

        Map<String, Object> result = vehiculoMutacionService.subirImagenes(vehicleId, files, actorId, request);
        return ApiResponse.success(result, "Imágenes subidas exitosamente");
    }

    private List<MultipartFile> extractImages(HttpServletRequest request) {
        if (!(request instanceof MultipartHttpServletRequest multipartRequest)) {
            return List.of();
        }
        List<MultipartFile> files = new ArrayList<>();
        List<MultipartFile> multi = multipartRequest.getFiles("images[]");
        if (multi != null && !multi.isEmpty()) {
            files.addAll(multi);
        }
        for (String param : List.of("image", "imagen", "file", "archivo")) {
            MultipartFile single = multipartRequest.getFile(param);
            if (single != null && !single.isEmpty()) {
                files.add(single);
            }
        }
        return files;
    }
}
