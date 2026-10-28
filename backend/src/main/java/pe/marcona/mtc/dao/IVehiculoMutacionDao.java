package pe.marcona.mtc.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contrato de acceso a datos para operaciones de cambio de estado y mutación de vehículos (CQRS - Command).
 */
public interface IVehiculoMutacionDao {

    boolean existePlaca(String placa, Long idExcluido);

    long insertarVehiculo(Long idEmpresa, String tipoPropiedad, String tipoServicio, String categoria,
                          String placa, String marca, String modelo, String color, Integer anioFabricacion,
                          String numeroVin, String numeroMotor, String combustible, String soatPoliza,
                          String soatAseguradora, LocalDate soatVencimiento, String rtEntidad,
                          String rtCertificado, LocalDate rtVencimiento);

    Optional<Map<String, Object>> obtenerPorId(long id);

    int actualizarVehiculo(long id, List<String> clausulas, List<Object> parametros);

    int alternarEstado(long id, String nuevoEstado);

    int contarImagenes(long idVehiculo);

    void insertarImagen(long idVehiculo, String nombreOriginal, String rutaRelativa, long tamanio, int orden);
}

