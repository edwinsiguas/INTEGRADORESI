package pe.marcona.mtc.service;

/**
 * Contrato de servicio para el control y limitación de tasa de solicitudes por IP y acción.
 */
public interface ILimitePeticionesService {

    void verificar(String accion, String ip, int maximoPermitido, int segundosVentana);
}
