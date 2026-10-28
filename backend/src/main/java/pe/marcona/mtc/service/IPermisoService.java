package pe.marcona.mtc.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Contrato de servicio para la gestión de permisos de circulación y autorizaciones.
 */
public interface IPermisoService {

    /**
     * Lista los permisos emitidos según los filtros de consulta con paginación.
     */
    Map<String, Object> listarPermisos(Map<String, String> consulta);

    /**
     * Obtiene el detalle integral de un permiso por su identificador.
     */
    Map<String, Object> obtenerPermisoPorId(long id);

    /**
     * Genera y emite un nuevo permiso de circulación validando asignaciones y vigencias.
     */
    Map<String, Object> generarPermiso(Map<String, Object> datosEntrada, long idActor, HttpServletRequest solicitud);

    /**
     * Revoca un permiso vigente registrando el motivo y la auditoría.
     */
    boolean revocarPermiso(long idPermiso, String motivo, long idActor, HttpServletRequest solicitud);

    /**
     * Renueva un permiso existente previo control de vigencias de SOAT, RT y licencias.
     */
    Map<String, Object> renovarPermiso(long idPermisoAntiguo, Map<String, Object> datosEntrada, long idActor, HttpServletRequest solicitud);

    /**
     * Obtiene las imágenes registradas del vehículo asociado.
     */
    List<Map<String, Object>> obtenerImagenesVehiculo(long idVehiculo);

    /**
     * Marca como vencidos los permisos cuya fecha límite ha expirado.
     */
    List<Long> marcarExpirados(long idActor, HttpServletRequest solicitud);

    /**
     * Genera la secuencia correlativa oficial del número de permiso para el año corriente.
     */
    String generarSiguienteNumeroPermiso();
}

