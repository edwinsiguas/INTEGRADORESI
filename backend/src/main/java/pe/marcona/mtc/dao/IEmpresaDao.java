package pe.marcona.mtc.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad Empresas.
 */
public interface IEmpresaDao {

    long contarEmpresas(String busqueda, String estado);

    long contarEmpresasActivas();

    List<Map<String, Object>> listarEmpresas(String busqueda, String estado, int limite, long desplazamiento);

    Optional<Map<String, Object>> obtenerPorId(long id);

    Optional<Map<String, Object>> obtenerPorRuc(String ruc);

    boolean existePorColumna(String columna, Object valor, Long idExcluido);

    void insertarEmpresa(String nombre, String ruc, String telefono, String email, String direccion, String contacto);

    int actualizarEmpresa(long id, String nombre, String ruc, String telefono, String email, String direccion, String contacto, String estado);

    int actualizarEstado(long id, String nuevoEstado);
}
