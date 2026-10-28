package pe.marcona.mtc.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Contrato de servicio para operaciones que alteran el estado o datos de vehículos (CQRS - Command).
 * Centraliza la creación, modificación, carga de imágenes y el cambio de estado operativo.
 */
public interface IVehiculoMutacionService {

    /**
     * Registra un nuevo vehículo validando unicidad de placa y procesando imágenes adjuntas.
     */
    Map<String, Object> crearVehiculo(Map<String, String> datos, List<MultipartFile> archivos, long idActor, HttpServletRequest solicitud);

    /**
     * Actualiza las características técnicas y administrativas de un vehículo existente.
     */
    Map<String, Object> actualizarVehiculo(Map<String, Object> datos, long idActor, HttpServletRequest solicitud);

    /**
     * Procesa, optimiza y almacena nuevas imágenes para un vehículo activo.
     */
    Map<String, Object> subirImagenes(long idVehiculo, List<MultipartFile> archivos, long idActor, HttpServletRequest solicitud);

    /**
     * Alterna el estado operativo de un vehículo (activo/inactivo).
     * Nota arquitectónica: Movido desde el servicio de consultas para cumplir estrictamente con CQRS y SRP.
     */
    Map<String, Object> alternarEstado(long idVehiculo, HttpServletRequest solicitud, long idActor);
}

