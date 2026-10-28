package pe.marcona.mtc.service.impl;

import com.google.common.base.Preconditions;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.marcona.mtc.dao.IPermisoDao;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.AuditService;
import pe.marcona.mtc.service.IPermisoService;
import pe.marcona.mtc.service.QrCodeService;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;

/**
 * Implementación de la lógica de negocio para la emisión, revocación y renovación de permisos.
 * Aplica Inversión de Dependencias (DIP) inyectando {@link IPermisoDao}.
 */
@Service
public class PermisoServiceImpl implements IPermisoService {

    private static final Logger log = LoggerFactory.getLogger(PermisoServiceImpl.class);

    private static final Set<String> TIPOS_VALIDOS = Set.of(
            "libre_transito", "ruta_fija", "carga", "especial");

    private final IPermisoDao permisoDao;
    private final QrCodeService qrCodeService;
    private final AuditService auditService;
    private final String appUrl;
    private final String apiUrl;

    public PermisoServiceImpl(
            IPermisoDao permisoDao,
            QrCodeService qrCodeService,
            AuditService auditService,
            @Value("${APP_URL:http://localhost:3000}") String appUrl,
            @Value("${app.api-url:http://localhost/api}") String apiUrl) {
        this.permisoDao = Preconditions.checkNotNull(permisoDao, "El DAO de permisos no puede ser nulo");
        this.qrCodeService = Preconditions.checkNotNull(qrCodeService, "El servicio de códigos QR no puede ser nulo");
        this.auditService = Preconditions.checkNotNull(auditService, "El servicio de auditoría no puede ser nulo");
        this.appUrl = (appUrl == null ? "http://localhost:3000" : appUrl).replaceAll("/+$", "");
        this.apiUrl = (apiUrl == null ? "http://localhost/api" : apiUrl).replaceAll("/+$", "");
    }

    @Override
    public Map<String, Object> listarPermisos(Map<String, String> consulta) {
        int pagina = enteroPositivo(consulta.get("page"), 1);
        int limite = enteroPositivo(consulta.get("limit"), 10);
        long desplazamiento = (long) (pagina - 1) * limite;

        String busqueda = texto(consulta.get("search"));
        String estado = texto(consulta.get("estado"));
        Long idVehiculo = parsearLongOpcional(consulta.get("vehicle_id"));
        Long idConductor = parsearLongOpcional(consulta.get("driver_id"));
        Long idEmpresa = parsearLongOpcional(consulta.get("company_id"));

        String proximoAVencerStr = texto(consulta.get("expiring_soon"));
        boolean proximoAVencer = "1".equals(proximoAVencerStr) || "true".equalsIgnoreCase(proximoAVencerStr);

        long totalRegistros = permisoDao.contarPermisos(busqueda, estado, idVehiculo, idConductor, idEmpresa, proximoAVencer);
        List<Map<String, Object>> filas = permisoDao.listarPermisos(
                        busqueda, estado, idVehiculo, idConductor, idEmpresa, proximoAVencer, limite, desplazamiento)
                .stream()
                .map(this::formatearFechas)
                .toList();

        long totalPaginas = totalRegistros == 0 ? 0 : (totalRegistros + limite - 1) / limite;

        return Map.of(
                "data", filas,
                "pagination", Map.of(
                        "page", pagina,
                        "limit", limite,
                        "total", totalRegistros,
                        "pages", totalPaginas));
    }

    @Override
    public Map<String, Object> obtenerPermisoPorId(long id) {
        return permisoDao.obtenerPorId(id)
                .map(this::formatearFechas)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Permiso " + id + " no encontrado"));
    }

    @Override
    @Transactional
    public Map<String, Object> generarPermiso(Map<String, Object> datosEntrada, long idActor, HttpServletRequest solicitud) {
        long idVehiculo = parsearLongPositivo(datosEntrada.get("vehicle_id"), "vehicle_id es obligatorio");
        String fechaVencimientoStr = texto(datosEntrada.get("fecha_vencimiento"));
        if (fechaVencimientoStr.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "fecha_vencimiento es obligatoria");
        }

        LocalDate fechaVencimiento;
        try {
            fechaVencimiento = LocalDate.parse(fechaVencimientoStr);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Fecha de vencimiento inválida (formato YYYY-MM-DD)");
        }

        if (!fechaVencimiento.isAfter(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha de vencimiento debe ser posterior a hoy");
        }

        String tipoPermiso = texto(datosEntrada.getOrDefault("tipo_permiso", "libre_transito"));
        if (!TIPOS_VALIDOS.contains(tipoPermiso)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tipo de permiso inválido. Use: " + String.join(", ", TIPOS_VALIDOS));
        }

        Map<String, Object> vehiculo = permisoDao.obtenerVehiculoPorId(idVehiculo)
                .filter(v -> "activo".equals(v.get("estado")))
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Vehículo no encontrado o inactivo"));

        Long idEmpresa = vehiculo.get("company_id") == null ? null : ((Number) vehiculo.get("company_id")).longValue();

        Map<String, Object> asignacion = permisoDao.obtenerAsignacionActivaPorVehiculo(idVehiculo)
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "El vehículo no tiene un conductor asignado. Asigne un conductor primero."));

        long idAsignacion = ((Number) asignacion.get("id")).longValue();
        long idConductor = ((Number) asignacion.get("driver_id")).longValue();

        LocalDate licenciaVence = convertirALocalDate(asignacion.get("fecha_vencimiento_licencia"));
        if (licenciaVence != null && licenciaVence.isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La licencia del conductor " + asignacion.get("nombre_completo") + " está vencida. Actualícela primero.");
        }

        int permisosVigentes = permisoDao.contarPermisosVigentesPorAsignacion(idAsignacion);
        if (permisosVigentes > 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Ya existe un permiso vigente para este vehículo con el conductor actual");
        }

        String numeroPermiso = generarSiguienteNumeroPermiso();
        String datosQr = appUrl + "/tuc/" + numeroPermiso;
        String qrBase64 = qrCodeService.generateBase64Qr(datosQr);

        String observaciones = texto(datosEntrada.get("observaciones"));
        String numeroRecibo = textoOpcional(datosEntrada.get("numero_recibo"));
        BigDecimal montoPagado = parsearBigDecimal(datosEntrada.get("monto_pagado"));

        long idPermisoGenerado = permisoDao.insertarPermiso(
                idAsignacion, idVehiculo, idConductor, idEmpresa,
                numeroPermiso, tipoPermiso, fechaVencimiento,
                observaciones, qrBase64, idActor,
                numeroRecibo, montoPagado);

        auditService.record(idActor, "generar_permiso", "permiso", idPermisoGenerado,
                "Permiso generado: " + numeroPermiso, solicitud);

        log.info("Permiso emitido exitosamente: id={}, numero={}", idPermisoGenerado, numeroPermiso);
        return obtenerPermisoPorId(idPermisoGenerado);
    }

    @Override
    @Transactional
    public boolean revocarPermiso(long idPermiso, String motivo, long idActor, HttpServletRequest solicitud) {
        String motivoLimpio = motivo == null || motivo.isBlank() ? "Revocado por administrador" : motivo.trim();

        boolean revocado = permisoDao.revocarPermiso(idPermiso, motivoLimpio);
        if (revocado) {
            auditService.record(idActor, "revocar_permiso", "permiso", idPermiso, motivoLimpio, solicitud);
            log.info("Permiso revocado: id={}, motivo={}", idPermiso, motivoLimpio);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public Map<String, Object> renovarPermiso(long idPermisoAntiguo, Map<String, Object> datosEntrada,
                                             long idActor, HttpServletRequest solicitud) {
        Map<String, Object> permisoAntiguo = obtenerPermisoPorId(idPermisoAntiguo);

        if ("revocado".equals(permisoAntiguo.get("estado"))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "No se puede renovar un permiso revocado");
        }

        long idVehiculo = ((Number) permisoAntiguo.get("vehicle_id")).longValue();
        long idConductor = ((Number) permisoAntiguo.get("driver_id")).longValue();
        Long idAsignacion = permisoAntiguo.get("assignment_id") == null ? null : ((Number) permisoAntiguo.get("assignment_id")).longValue();
        Long idEmpresa = permisoAntiguo.get("company_id") == null ? null : ((Number) permisoAntiguo.get("company_id")).longValue();

        Map<String, Object> vehiculo = permisoDao.obtenerVehiculoPorId(idVehiculo)
                .filter(v -> "activo".equals(v.get("estado")))
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "El vehículo está inactivo"));

        LocalDate soatVence = convertirALocalDate(vehiculo.get("soat_vencimiento"));
        if (soatVence == null || soatVence.isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "El vehículo no tiene SOAT vigente");
        }

        LocalDate rtVence = convertirALocalDate(vehiculo.get("rt_vencimiento"));
        if (rtVence == null || rtVence.isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "El vehículo no tiene Revisión Técnica vigente");
        }

        Map<String, Object> conductor = permisoDao.obtenerConductorPorId(idConductor)
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Conductor no encontrado"));

        LocalDate licenciaVence = convertirALocalDate(conductor.get("fecha_vencimiento_licencia"));
        if (licenciaVence == null || licenciaVence.isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "La licencia del conductor está vencida");
        }

        if (idAsignacion != null) {
            int asignacionesActivas = permisoDao.contarAsignacionesActivasPorId(idAsignacion);
            if (asignacionesActivas == 0) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "La asignación vinculada a este permiso ya no está activa");
            }
        }

        permisoDao.actualizarEstado(idPermisoAntiguo, "renovado");

        LocalDate nuevoVencimiento = LocalDate.now().plusYears(1);
        String numeroPermiso = generarSiguienteNumeroPermiso();
        String datosQr = appUrl + "/tuc/" + numeroPermiso;
        String qrBase64 = qrCodeService.generateBase64Qr(datosQr);

        String obsInput = texto(datosEntrada.get("observaciones"));
        final String observacionesLimpias = obsInput.isBlank()
                ? "Renovación de permiso " + permisoAntiguo.get("numero_permiso")
                : obsInput;
        String numeroRecibo = textoOpcional(datosEntrada.get("numero_recibo"));
        BigDecimal montoPagado = parsearBigDecimal(datosEntrada.get("monto_pagado"));
        String tipoPermiso = String.valueOf(permisoAntiguo.getOrDefault("tipo_permiso", "libre_transito"));

        long idNuevoPermiso = permisoDao.insertarPermiso(
                idAsignacion, idVehiculo, idConductor, idEmpresa,
                numeroPermiso, tipoPermiso, nuevoVencimiento,
                observacionesLimpias, qrBase64, idActor,
                numeroRecibo, montoPagado);

        auditService.record(idActor, "renovar_permiso", "permiso", idNuevoPermiso,
                "Permiso renovado a partir del ID " + idPermisoAntiguo, solicitud);

        log.info("Permiso renovado: idAntiguo={}, idNuevo={}", idPermisoAntiguo, idNuevoPermiso);
        return obtenerPermisoPorId(idNuevoPermiso);
    }

    @Override
    public List<Map<String, Object>> obtenerImagenesVehiculo(long idVehiculo) {
        List<Map<String, Object>> filas = permisoDao.obtenerImagenesVehiculo(idVehiculo);
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map<String, Object> r : filas) {
            String ruta = String.valueOf(r.get("file_path")).replaceFirst("^/+", "");
            resultado.add(Map.of(
                    "url", apiUrl + "/" + ruta,
                    "sort_order", ((Number) r.get("sort_order")).intValue()));
        }
        return resultado;
    }

    @Override
    @Transactional
    public List<Long> marcarExpirados(long idActor, HttpServletRequest solicitud) {
        List<Long> idsExpirados = permisoDao.obtenerIdsPermisosVencidosParaBloqueo();
        if (idsExpirados.isEmpty()) {
            return List.of();
        }

        for (Long id : idsExpirados) {
            permisoDao.marcarComoVencido(id);
            auditService.record(idActor, "vencimiento_automatico", "permiso", id,
                    "Permiso vencido automáticamente por fecha de vencimiento", solicitud);
        }

        log.info("Permisos vencidos automáticamente marcados: {}", idsExpirados.size());
        return idsExpirados;
    }

    @Override
    @Transactional
    public synchronized String generarSiguienteNumeroPermiso() {
        int anio = Year.now().getValue();
        int correlativo = permisoDao.obtenerSiguienteSecuencia(anio);
        return String.format("PERM-%d-%05d", anio, correlativo);
    }

    private Map<String, Object> formatearFechas(Map<String, Object> fila) {
        var salida = new LinkedHashMap<String, Object>(fila);
        salida.replaceAll((clave, valor) -> {
            if (valor instanceof Timestamp timestamp) {
                return timestamp.toLocalDateTime().toString().replace('T', ' ');
            }
            if (valor instanceof Date date) {
                return date.toLocalDate().toString();
            }
            return valor;
        });
        return salida;
    }

    private LocalDate convertirALocalDate(Object valor) {
        if (valor instanceof Date date) return date.toLocalDate();
        if (valor instanceof LocalDate localDate) return localDate;
        if (valor != null && !valor.toString().isBlank()) {
            try {
                return LocalDate.parse(valor.toString());
            } catch (Exception ignored) { }
        }
        return null;
    }

    private BigDecimal parsearBigDecimal(Object valor) {
        if (valor == null || valor.toString().isBlank()) return null;
        try {
            return new BigDecimal(valor.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private long parsearLongPositivo(Object valor, String mensajeError) {
        if (valor == null) throw new ApiException(HttpStatus.BAD_REQUEST, mensajeError);
        try {
            long parseado = Long.parseLong(valor.toString().trim());
            if (parseado <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, mensajeError);
            return parseado;
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, mensajeError);
        }
    }

    private Long parsearLongOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int enteroPositivo(String valor, int valorPorDefecto) {
        if (valor == null || valor.isBlank()) return valorPorDefecto;
        try {
            return Math.max(1, Integer.parseInt(valor));
        } catch (NumberFormatException e) {
            return valorPorDefecto;
        }
    }

    private String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }

    private String textoOpcional(Object valor) {
        String s = texto(valor);
        return s.isBlank() ? null : s;
    }
}
