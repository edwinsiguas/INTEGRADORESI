package pe.marcona.mtc.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.marcona.mtc.dao.ICirculacionDao;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.ICirculacionService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CirculacionServiceImpl implements ICirculacionService {

    private final ICirculacionDao circulacionDao;

    public CirculacionServiceImpl(ICirculacionDao circulacionDao) {
        this.circulacionDao = circulacionDao;
    }

    @Override
    public Map<String, Object> validar(Map<String, Object> entrada) {
        String placa = valor(entrada.get("placa"));
        String dni = valor(entrada.get("dni"));
        String numeroPermiso = valor(entrada.get("numero_permiso"));

        if (!placa.isBlank()) {
            return validarPorPlaca(placa);
        }
        if (!dni.isBlank()) {
            if (!dni.matches("\\d{8}")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "dni debe tener 8 dígitos");
            }
            return validarPorDni(dni);
        }
        if (!numeroPermiso.isBlank()) {
            return validarPorPermiso(numeroPermiso);
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Proporcione placa, dni o numero_permiso");
    }

    private Map<String, Object> validarPorPlaca(String placa) {
        Optional<Map<String, Object>> filaOpt = circulacionDao.buscarPorPlaca(placa);
        if (filaOpt.isEmpty()) {
            return construirResultado(false, null, List.of("Vehículo no encontrado o sin conductor asignado actualmente"));
        }
        return evaluar(filaOpt.get());
    }

    private Map<String, Object> validarPorDni(String dni) {
        Optional<Map<String, Object>> filaOpt = circulacionDao.buscarPorDni(dni);
        if (filaOpt.isEmpty()) {
            return construirResultado(false, null, List.of("Conductor no encontrado o sin vehículo asignado actualmente"));
        }
        return evaluar(filaOpt.get());
    }

    private Map<String, Object> validarPorPermiso(String numeroPermiso) {
        Optional<Map<String, Object>> filaOpt = circulacionDao.buscarPorNumeroPermiso(numeroPermiso);
        if (filaOpt.isEmpty()) {
            return construirResultado(false, null, List.of("Permiso no encontrado"));
        }
        return evaluar(filaOpt.get());
    }

    private Map<String, Object> evaluar(Map<String, Object> fila) {
        List<String> bloqueos = new ArrayList<>();
        if (fila.get("empresa") != null && !"activo".equals(fila.get("estado_empresa"))) {
            bloqueos.add("La empresa operadora (" + fila.get("empresa") + ") se encuentra SUSPENDIDA.");
        }
        if (!"activo".equals(fila.get("estado_vehiculo"))) {
            bloqueos.add("Vehículo suspendido o inactivo");
        }
        if (!"activo".equals(fila.get("estado_conductor"))) {
            bloqueos.add("Conductor suspendido o inactivo");
        }
        String vencimientoLicencia = valorFecha(fila.get("fecha_vencimiento_licencia"));
        if (!vencimientoLicencia.isBlank() && LocalDate.parse(vencimientoLicencia).isBefore(LocalDate.now())) {
            bloqueos.add("Licencia de conducir vencida (" + vencimientoLicencia + ")");
        }
        if (fila.get("permit_id") == null && fila.get("numero_permiso") == null) {
            bloqueos.add("Sin permiso de libre tránsito activo vigente");
        }
        if ("vencido".equals(fila.get("estado_permiso"))) {
            bloqueos.add("Permiso de libre tránsito vencido (" + valorFecha(fila.get("permiso_vence")) + ")");
        }
        return construirResultado(bloqueos.isEmpty(), fila, bloqueos);
    }

    private Map<String, Object> construirResultado(boolean permitido, Map<String, Object> datos, List<String> bloqueos) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("puede_circular", permitido);
        resultado.put("razon", permitido ? "Habilitado para circular correctamente" : String.join(". ", bloqueos));
        resultado.put("bloqueos", bloqueos);
        resultado.put("verificado", permitido);
        resultado.put("data", datos);
        return resultado;
    }

    private String valorFecha(Object valor) {
        if (valor instanceof Date fecha) {
            return fecha.toLocalDate().toString();
        }
        return valor == null ? "" : valor.toString();
    }

    private String valor(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }
}

