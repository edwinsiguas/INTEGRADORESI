package pe.marcona.mtc.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad de Usuarios.
 */
public interface IUsuarioDao {

    long contarUsuarios(String busqueda, String rol, String estado, boolean incluirInactivos);

    List<Map<String, Object>> listarUsuarios(String busqueda, String rol, String estado, boolean incluirInactivos, int limite, long desplazamiento);

    void insertarUsuario(String email, String passwordEncriptada, String nombre, String rol);

    Optional<Map<String, Object>> obtenerPorEmail(String email);

    Optional<Map<String, Object>> obtenerPorId(Long id);

    boolean existePorEmail(String email, Long idExcluido);

    int actualizarCampos(Long id, Map<String, Object> campos);
}

