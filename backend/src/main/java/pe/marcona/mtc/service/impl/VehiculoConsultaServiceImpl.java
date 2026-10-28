package pe.marcona.mtc.service.impl;

import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.marcona.mtc.dao.IVehiculoConsultaDao;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.IVehiculoConsultaService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación especializada estrictamente en consultas y búsquedas de vehículos (CQRS - Query).
 * Garantiza inmutabilidad del estado mediante transacción de solo lectura.
 */
@Service
@Transactional(readOnly = true)
public class VehiculoConsultaServiceImpl implements IVehiculoConsultaService {

    private final IVehiculoConsultaDao vehiculoConsultaDao;
    private final String apiUrl;

    public VehiculoConsultaServiceImpl(
            IVehiculoConsultaDao vehiculoConsultaDao,
            @Value("${app.api-url:http://localhost/api}") String apiUrl) {
        this.vehiculoConsultaDao = Preconditions.checkNotNull(vehiculoConsultaDao, "El DAO de consulta de vehículos no puede ser nulo");
        this.apiUrl = (apiUrl == null ? "http://localhost/api" : apiUrl).replaceAll("/+$", "");
    }

    @Override
    public Map<String, Object> listar(Map<String, String> consulta) {
        int pagina = enteroPositivo(consulta.get("page"), 1);
        int limite = enteroPositivo(consulta.get("limit"), 10);
        String busqueda = texto(consulta.get("search"));
        Long idEmpresa = parsearIdOpcional(consulta.get("company_id"));
        Long idConductor = parsearIdOpcional(consulta.get("driver_id"));
        String estado = texto(consulta.get("estado"));
        boolean incluirInactivos = esVerdadero(consulta.get("include_inactive"));

        long totalRegistros = vehiculoConsultaDao.contarVehiculos(busqueda, idEmpresa, idConductor, estado, incluirInactivos);
        long desplazamiento = (long) (pagina - 1) * limite;

        List<Map<String, Object>> vehiculos = vehiculoConsultaDao.listarVehiculos(
                        busqueda, idEmpresa, idConductor, estado, incluirInactivos, limite, desplazamiento)
                .stream()
                .map(this::formatearImagen)
                .toList();

        return Map.of(
                "data", vehiculos,
                "pagination", Map.of(
                        "page", pagina,
                        "limit", limite,
                        "total", totalRegistros,
                        "pages", (totalRegistros + limite - 1) / limite
                )
        );
    }

    private Map<String, Object> formatearImagen(Map<String, Object> fila) {
        var salida = new LinkedHashMap<String, Object>(fila);
        Object imagen = salida.get("primary_image");
        if (imagen != null && !imagen.toString().isBlank()) {
            salida.put("primary_image", apiUrl + "/" + imagen.toString().replaceFirst("^/+", ""));
        }
        return salida;
    }

    private int enteroPositivo(String valor, int porDefecto) {
        if (valor == null || valor.isBlank()) return porDefecto;
        try {
            return Math.max(1, Integer.parseInt(valor));
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Parámetro de paginación inválido");
        }
    }

    private Long parsearIdOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            long id = Long.parseLong(valor.trim());
            if (id > 0) return id;
        } catch (NumberFormatException ignored) { }
        throw new ApiException(HttpStatus.BAD_REQUEST, "El identificador debe ser numérico");
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private boolean esVerdadero(String valor) {
        return valor != null && !valor.isBlank() && !"0".equals(valor);
    }
}
