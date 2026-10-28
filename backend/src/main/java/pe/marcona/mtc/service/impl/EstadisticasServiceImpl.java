package pe.marcona.mtc.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.marcona.mtc.dao.IEstadisticasDao;
import pe.marcona.mtc.service.IEstadisticasService;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class EstadisticasServiceImpl implements IEstadisticasService {

    private static final List<String> MESES = List.of(
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic");

    private static final List<String> COLORES = List.of(
            "#0A2342", "#3B82F6", "#93C5FD", "#F59E0B", "#10B981");

    private final IEstadisticasDao estadisticasDao;

    public EstadisticasServiceImpl(IEstadisticasDao estadisticasDao) {
        this.estadisticasDao = estadisticasDao;
    }

    @Override
    public Map<String, Object> obtenerDashboard() {
        long vehiculos = estadisticasDao.contarVehiculos();
        long empresas = estadisticasDao.contarEmpresas();
        long conductores = estadisticasDao.contarConductores();
        long permisosActivos = estadisticasDao.contarPermisosVigentes();
        long permisosPorVencer = estadisticasDao.contarPermisosPorVencer(30);
        long vehiculosActivos = estadisticasDao.contarVehiculosPorEstado("activo");
        long vehiculosInactivos = estadisticasDao.contarVehiculosPorEstado("inactivo");

        List<Map<String, Object>> filasTramites = estadisticasDao.obtenerTramitesPorMesUltimoSemestre();
        List<Map<String, Object>> tramites = filasTramites.stream().map(fila -> {
            int mesNumero = ((Number) fila.get("month_number")).intValue();
            String nombreMes = MESES.get(mesNumero - 1).toUpperCase(Locale.ROOT);
            return Map.of(
                    "name", (Object) nombreMes,
                    "aprobados", fila.get("aprobados"));
        }).toList();

        return Map.of(
                "vehicles", vehiculos,
                "companies", empresas,
                "drivers", conductores,
                "permits_active", permisosActivos,
                "permits_expiring", permisosPorVencer,
                "inspections_today", 0,
                "flota_chart", List.of(
                        Map.of("name", "Habilitados", "value", vehiculosActivos),
                        Map.of("name", "Suspendidos", "value", vehiculosInactivos)),
                "tramites_chart", tramites,
                "recent_activity", List.of(Map.of(
                        "id", 1, "action", "Registro de la plataforma inicial",
                        "time", "Recién", "type", "system", "user", "Admin", "initials", "AD")),
                "upcoming_expirations", List.of());
    }

    @Override
    public Map<String, Object> obtenerEmpresas() {
        long vehiculos = estadisticasDao.contarVehiculos();
        long empresasActivas = estadisticasDao.contarEmpresasPorEstado("activo");
        long porVencer = estadisticasDao.contarPermisosPorVencer(30);
        return Map.of("vehicles", vehiculos, "rutas", empresasActivas * 2, "proximos", porVencer);
    }

    @Override
    public Map<String, Object> obtenerAnalitica() {
        long flotaActiva = estadisticasDao.contarVehiculosPorEstado("activo");
        long soatVigente = estadisticasDao.contarVehiculosSoatVigente();
        long tramitesMes = estadisticasDao.contarTramitesMesActual();
        long empresasActivas = estadisticasDao.contarEmpresasPorEstado("activo");
        double porcentajeSoat = flotaActiva == 0 ? 0 : Math.round((double) soatVigente / flotaActiva * 1000) / 10.0;

        Map<String, Object> kpiData = Map.of(
                "flota_autorizada", flotaActiva,
                "cumplimiento_soat", porcentajeSoat,
                "tramites_proceso", tramitesMes,
                "empresas_activas", empresasActivas);

        long rtVigente = estadisticasDao.contarVehiculosRtVigente();
        long conductoresActivos = estadisticasDao.contarConductoresPorEstado("activo");
        long licenciasVigentes = estadisticasDao.contarConductoresLicenciaVigente();

        Map<String, Object> documentation = Map.of(
                "soat", Map.of("valid", soatVigente, "total", flotaActiva),
                "rt", Map.of("valid", rtVigente, "total", flotaActiva),
                "licencias", Map.of("valid", licenciasVigentes, "total", conductoresActivos));

        List<Map<String, Object>> growthData = calcularCrecimiento();
        List<Map<String, Object>> serviceData = calcularDistribucionServicio();
        List<Map<String, Object>> rankingData = calcularRankingEmpresas();

        return Map.of(
                "kpis", kpiData,
                "documentation", documentation,
                "growthData", growthData,
                "serviceData", serviceData,
                "rankingData", rankingData);
    }

    private List<Map<String, Object>> calcularCrecimiento() {
        List<Map<String, Object>> filasCrecimiento = estadisticasDao.obtenerCrecimientoVehicularPorMes();
        List<Map<String, Object>> mesesSecuencia = estadisticasDao.obtenerMesesSecuencia();

        Map<String, Map<String, Object>> agrupadores = new LinkedHashMap<>();
        for (Map<String, Object> mes : mesesSecuencia) {
            String clave = String.valueOf(mes.get("month_key"));
            YearMonth anioMes = YearMonth.parse(clave);
            String nombreMes = MESES.get(anioMes.getMonthValue() - 1).toUpperCase(Locale.ROOT);
            agrupadores.put(clave, new LinkedHashMap<>(Map.of(
                    "name", nombreMes,
                    "vehiculos", 0L,
                    "mototaxis", 0L)));
        }

        for (Map<String, Object> fila : filasCrecimiento) {
            Map<String, Object> agrupador = agrupadores.get(String.valueOf(fila.get("month_key")));
            if (agrupador == null) {
                continue;
            }
            String categoria = String.valueOf(fila.get("categoria"));
            String propiedad = "moto-taxi".equalsIgnoreCase(categoria) ? "mototaxis" : "vehiculos";
            long actual = ((Number) agrupador.get(propiedad)).longValue();
            agrupador.put(propiedad, actual + ((Number) fila.get("total")).longValue());
        }
        return new ArrayList<>(agrupadores.values());
    }

    private List<Map<String, Object>> calcularDistribucionServicio() {
        List<Map<String, Object>> filas = estadisticasDao.obtenerDistribucionTipoServicio();
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            Map<String, Object> fila = filas.get(i);
            int indiceColor = i % COLORES.size();
            String nombre = String.valueOf(fila.get("tipo_servicio"));
            String formateado = nombre.isEmpty() ? nombre : nombre.substring(0, 1).toUpperCase() + nombre.substring(1);
            resultado.add(Map.of(
                    "name", formateado,
                    "value", fila.get("total"),
                    "color", COLORES.get(indiceColor)));
        }
        return resultado;
    }

    private List<Map<String, Object>> calcularRankingEmpresas() {
        List<Map<String, Object>> filas = estadisticasDao.obtenerRankingEmpresasFlota();
        return filas.stream().map(fila -> {
            long flota = ((Number) fila.get("fleet")).longValue();
            long valido = ((Number) fila.get("valid_soat")).longValue();
            int cumplimiento = flota == 0 ? 0 : (int) Math.round((double) valido / flota * 100);
            String nombre = String.valueOf(fila.get("nombre"));
            return Map.of(
                    "name", (Object) nombre,
                    "ruc", fila.get("ruc"),
                    "flota", flota,
                    "categoria", "Transporte",
                    "cumplimiento", cumplimiento,
                    "isWarning", cumplimiento < 80,
                    "logo", nombre.substring(0, Math.min(2, nombre.length())).toUpperCase());
        }).toList();
    }
}

