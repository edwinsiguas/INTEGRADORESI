package pe.marcona.mtc.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad Conductores.
 */
public interface IConductorDao {

    long contarConductores(String busqueda, Long idEmpresa, String estado, boolean incluirInactivos);

    List<Map<String, Object>> listarConductores(String busqueda, Long idEmpresa, String estado, boolean incluirInactivos, int limite, long desplazamiento);

    Optional<Map<String, Object>> obtenerPorId(long id);

    Optional<Map<String, Object>> obtenerPorDni(String dni);

    boolean existePorColumna(String columna, Object valor, Long idExcluido);

    void insertarConductor(Long idUsuario, Long idEmpresa, String nombreCompleto, String dni, String telefono, String numeroLicencia, LocalDate fechaVencimientoLicencia);

    int actualizarConductor(long id, Map<String, Object> campos);
}
