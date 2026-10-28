package pe.marcona.mtc.dao;

/**
 * Contrato de acceso a datos para el control de tasa y límites de peticiones.
 */
public interface ILimitePeticionesDao {

    int contarPeticionesEnVentana(String ip, String accion, int segundosVentana);

    Long obtenerSegundosRestantes(String ip, String accion, int segundosVentana);

    void registrarPeticion(String ip, String accion);
}

