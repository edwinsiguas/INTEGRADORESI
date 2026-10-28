package pe.marcona.mtc.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.marcona.mtc.dao.IExportacionDao;
import pe.marcona.mtc.service.IExportacionService;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ExportacionServiceImpl implements IExportacionService {

    private final IExportacionDao exportacionDao;

    public ExportacionServiceImpl(IExportacionDao exportacionDao) {
        this.exportacionDao = exportacionDao;
    }

    @Override
    public List<Map<String, Object>> exportarEmpresas(Map<String, String> consulta) {
        List<String> condiciones = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();

        String estado = consulta.get("estado");
        if (estado != null && !estado.isBlank() && !"todos".equalsIgnoreCase(estado)) {
            condiciones.add("c.estado = ?");
            parametros.add(estado.trim());
        }

        String busqueda = consulta.get("search");
        if (busqueda != null && !busqueda.isBlank()) {
            condiciones.add("(c.nombre LIKE ? OR c.ruc LIKE ? OR c.contacto LIKE ?)");
            String termino = "%" + busqueda.trim() + "%";
            parametros.add(termino);
            parametros.add(termino);
            parametros.add(termino);
        }

        String where = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
        return exportacionDao.consultarEmpresasParaExportar(where, parametros)
                .stream().map(this::formatearFila).toList();
    }

    @Override
    public List<Map<String, Object>> exportarConductores(Map<String, String> consulta) {
        List<String> condiciones = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();

        String estado = consulta.get("estado");
        if (estado != null && !estado.isBlank() && !"todos".equalsIgnoreCase(estado)) {
            condiciones.add("d.estado = ?");
            parametros.add(estado.trim());
        }

        String empresa = consulta.get("empresa");
        if (empresa != null && !empresa.isBlank() && !"todas".equalsIgnoreCase(empresa)) {
            try {
                long idEmpresa = Long.parseLong(empresa.trim());
                condiciones.add("d.company_id = ?");
                parametros.add(idEmpresa);
            } catch (NumberFormatException ignored) { }
        }

        String where = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
        return exportacionDao.consultarConductoresParaExportar(where, parametros)
                .stream().map(this::formatearFila).toList();
    }

    @Override
    public List<Map<String, Object>> exportarVehiculos(Map<String, String> consulta) {
        List<String> condiciones = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();

        String estado = consulta.get("estado");
        if (estado != null && !estado.isBlank() && !"todos".equalsIgnoreCase(estado)) {
            condiciones.add("v.estado = ?");
            parametros.add(estado.trim());
        }

        String busqueda = consulta.get("search");
        if (busqueda != null && !busqueda.isBlank()) {
            condiciones.add("(v.placa LIKE ? OR c.nombre LIKE ? OR d.nombre_completo LIKE ?)");
            String termino = "%" + busqueda.trim() + "%";
            parametros.add(termino);
            parametros.add(termino);
            parametros.add(termino);
        }

        String where = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
        return exportacionDao.consultarVehiculosParaExportar(where, parametros)
                .stream().map(this::formatearFila).toList();
    }

    @Override
    public List<Map<String, Object>> exportarPermisos(Map<String, String> consulta) {
        List<String> condiciones = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();

        String estado = consulta.get("estado");
        if (estado != null && !estado.isBlank() && !"todos".equalsIgnoreCase(estado)) {
            condiciones.add("p.estado = ?");
            parametros.add(estado.trim());
        }

        String busqueda = consulta.get("search");
        if (busqueda != null && !busqueda.isBlank()) {
            condiciones.add("(p.numero_permiso LIKE ? OR v.placa LIKE ? OR c.nombre LIKE ?)");
            String termino = "%" + busqueda.trim() + "%";
            parametros.add(termino);
            parametros.add(termino);
            parametros.add(termino);
        }

        String where = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
        return exportacionDao.consultarPermisosParaExportar(where, parametros)
                .stream().map(this::formatearFila).toList();
    }

    private Map<String, Object> formatearFila(Map<String, Object> fila) {
        var salida = new LinkedHashMap<String, Object>(fila);
        salida.replaceAll((clave, valor) -> {
            if (valor instanceof Timestamp marcaTiempo) {
                return marcaTiempo.toLocalDateTime().toString().replace('T', ' ');
            }
            if (valor instanceof Date fecha) {
                return fecha.toLocalDate().toString();
            }
            return valor;
        });
        return salida;
    }
}
