package pe.marcona.mtc.service.impl;

import com.google.common.base.Preconditions;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.marcona.mtc.dao.IVehiculoMutacionDao;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.AuditService;
import pe.marcona.mtc.service.IVehiculoMutacionService;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

/**
 * Implementación de la lógica de negocio para mutaciones de vehículos (CQRS - Command).
 * Aplica Inversión de Dependencias (DIP) inyectando {@link IVehiculoMutacionDao}.
 */
@Service
public class VehiculoMutacionServiceImpl implements IVehiculoMutacionService {

    private static final Logger log = LoggerFactory.getLogger(VehiculoMutacionServiceImpl.class);
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp");

    private final IVehiculoMutacionDao vehiculoMutacionDao;
    private final AuditService auditService;
    private final Path raizSubidas;

    public VehiculoMutacionServiceImpl(
            IVehiculoMutacionDao vehiculoMutacionDao,
            AuditService auditService,
            @Value("${app.uploads-dir:uploads}") String uploadsDir) {
        this.vehiculoMutacionDao = Preconditions.checkNotNull(vehiculoMutacionDao, "El DAO de mutación de vehículos no puede ser nulo");
        this.auditService = Preconditions.checkNotNull(auditService, "El servicio de auditoría no puede ser nulo");
        this.raizSubidas = Path.of(uploadsDir).toAbsolutePath().normalize();
    }

    @Override
    @Transactional
    public Map<String, Object> crearVehiculo(
            Map<String, String> datos,
            List<MultipartFile> archivos,
            long idActor,
            HttpServletRequest solicitud) {

        String placa = textoRequerido(datos.get("placa"), "placa", 6, 10).toUpperCase();
        String categoria = textoRequerido(datos.get("categoria"), "categoria", 1, 20);
        String marca = textoRequerido(datos.get("marca"), "marca", 2, 50);
        String modelo = textoRequerido(datos.get("modelo"), "modelo", 2, 50);
        String color = textoRequerido(datos.get("color"), "color", 3, 30);

        Long idEmpresa = parsearLongOpcional(datos.get("company_id"), "El campo company_id debe ser numérico");

        if (vehiculoMutacionDao.existePlaca(placa, null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La placa ya está registrada");
        }

        String tipoPropiedad = "particular".equalsIgnoreCase(datos.get("tipo_propiedad")) ? "particular" : "empresa";
        String tipoServicio = textoOpcional(datos.get("tipo_servicio"));
        Integer anoFabricacion = parsearEnteroOpcional(datos.get("ano_fabricacion"));
        String numeroVin = textoOpcional(datos.get("numero_vin"));
        String numeroMotor = textoOpcional(datos.get("numero_motor"));
        String combustible = textoOpcional(datos.get("combustible"));
        String soatPoliza = textoOpcional(datos.get("soat_poliza"));
        String soatAseguradora = textoOpcional(datos.get("soat_aseguradora"));
        LocalDate soatVencimiento = parsearFechaOpcional(datos.get("soat_vencimiento"));
        String rtEntidad = textoOpcional(datos.get("rt_entidad"));
        String rtCertificado = textoOpcional(datos.get("rt_certificado"));
        LocalDate rtVencimiento = parsearFechaOpcional(datos.get("rt_vencimiento"));

        long idVehiculo = vehiculoMutacionDao.insertarVehiculo(
                idEmpresa, tipoPropiedad, tipoServicio, categoria, placa, marca, modelo, color,
                anoFabricacion, numeroVin, numeroMotor, combustible, soatPoliza, soatAseguradora,
                soatVencimiento, rtEntidad, rtCertificado, rtVencimiento);

        List<String> imagenesAlmacenadas = procesarYGuardarImagenes(idVehiculo, archivos, 0);

        auditService.record(idActor, "crear", "vehiculo", idVehiculo, "Vehículo creado: " + placa, solicitud);
        log.info("Vehículo creado exitosamente: id={}, placa={}", idVehiculo, placa);

        return Map.of(
                "id", idVehiculo,
                "placa", placa,
                "images", imagenesAlmacenadas
        );
    }

    @Override
    @Transactional
    public Map<String, Object> actualizarVehiculo(
            Map<String, Object> datos,
            long idActor,
            HttpServletRequest solicitud) {

        Long idVehiculo = parsearLongOpcional(datos.get("id"), "ID inválido");
        if (idVehiculo == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ID inválido");
        }

        if (vehiculoMutacionDao.obtenerPorId(idVehiculo).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Vehículo no encontrado");
        }

        List<String> clausulas = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();

        for (String fkOpcional : List.of("driver_id", "company_id")) {
            if (datos.containsKey(fkOpcional)) {
                Long fkValor = parsearLongOpcional(datos.get(fkOpcional), "El campo " + fkOpcional + " debe ser numérico");
                clausulas.add(fkOpcional + " = ?");
                parametros.add(fkValor);
            }
        }

        String nuevaPlaca = textoOpcional(datos.get("placa"));
        if (nuevaPlaca != null) {
            nuevaPlaca = nuevaPlaca.toUpperCase();
            if (vehiculoMutacionDao.existePlaca(nuevaPlaca, idVehiculo)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La placa ya está registrada");
            }
            clausulas.add("placa = ?");
            parametros.add(nuevaPlaca);
        }

        agregarCampoDirecto("marca", datos, clausulas, parametros);
        agregarCampoDirecto("modelo", datos, clausulas, parametros);
        agregarCampoDirecto("color", datos, clausulas, parametros);
        agregarCampoDirecto("categoria", datos, clausulas, parametros);
        agregarCampoDirecto("tipo_propiedad", datos, clausulas, parametros);
        agregarCampoDirecto("tipo_servicio", datos, clausulas, parametros);
        agregarCampoDirecto("numero_vin", datos, clausulas, parametros);
        agregarCampoDirecto("numero_motor", datos, clausulas, parametros);
        agregarCampoDirecto("combustible", datos, clausulas, parametros);
        agregarCampoDirecto("soat_poliza", datos, clausulas, parametros);
        agregarCampoDirecto("soat_aseguradora", datos, clausulas, parametros);
        agregarCampoDirecto("rt_entidad", datos, clausulas, parametros);
        agregarCampoDirecto("rt_certificado", datos, clausulas, parametros);

        if (datos.containsKey("ano_fabricacion")) {
            clausulas.add("ano_fabricacion = ?");
            parametros.add(parsearEnteroOpcional(datos.get("ano_fabricacion")));
        }
        if (datos.containsKey("soat_vencimiento")) {
            LocalDate d = parsearFechaOpcional(datos.get("soat_vencimiento"));
            clausulas.add("soat_vencimiento = ?");
            parametros.add(d != null ? Date.valueOf(d) : null);
        }
        if (datos.containsKey("rt_vencimiento")) {
            LocalDate d = parsearFechaOpcional(datos.get("rt_vencimiento"));
            clausulas.add("rt_vencimiento = ?");
            parametros.add(d != null ? Date.valueOf(d) : null);
        }
        if (datos.containsKey("estado")) {
            String estado = texto(datos.get("estado"));
            if (!Set.of("activo", "inactivo").contains(estado)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Estado inválido");
            }
            clausulas.add("estado = ?");
            parametros.add(estado);
        }

        if (clausulas.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No hay campos para actualizar");
        }

        clausulas.add("updated_at = NOW()");
        vehiculoMutacionDao.actualizarVehiculo(idVehiculo, clausulas, parametros);

        auditService.record(idActor, "actualizar", "vehiculo", idVehiculo, "Vehículo actualizado", solicitud);
        log.info("Vehículo actualizado: id={}", idVehiculo);

        return vehiculoMutacionDao.obtenerPorId(idVehiculo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));
    }

    @Override
    @Transactional
    public Map<String, Object> subirImagenes(
            long idVehiculo,
            List<MultipartFile> archivos,
            long idActor,
            HttpServletRequest solicitud) {

        Map<String, Object> vehiculo = vehiculoMutacionDao.obtenerPorId(idVehiculo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));

        if (!"activo".equals(vehiculo.get("estado"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Solo se pueden subir imágenes a vehículos activos");
        }

        if (archivos == null || archivos.isEmpty() || archivos.stream().allMatch(MultipartFile::isEmpty)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debe enviar al menos una imagen");
        }

        int totalImagenesActual = vehiculoMutacionDao.contarImagenes(idVehiculo);
        long cantidadNuevas = archivos.stream().filter(f -> f != null && !f.isEmpty()).count();
        if (totalImagenesActual + cantidadNuevas > 4) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El vehículo ya alcanzó el máximo de 4 imágenes");
        }

        List<String> almacenadas = procesarYGuardarImagenes(idVehiculo, archivos, totalImagenesActual);

        auditService.record(idActor, "subir_imagen_vehiculo", "vehiculo", idVehiculo,
                "Imágenes cargadas para vehículo ID " + idVehiculo, solicitud);

        log.info("Imágenes subidas para vehículo id={}: {}", idVehiculo, almacenadas.size());

        return Map.of(
                "vehicle_id", idVehiculo,
                "images", almacenadas
        );
    }

    @Override
    @Transactional
    public Map<String, Object> alternarEstado(long idVehiculo, HttpServletRequest solicitud, long idActor) {
        Map<String, Object> vehiculo = vehiculoMutacionDao.obtenerPorId(idVehiculo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));

        String nuevoEstado = "activo".equals(vehiculo.get("estado")) ? "inactivo" : "activo";
        vehiculoMutacionDao.alternarEstado(idVehiculo, nuevoEstado);

        auditService.record(idActor, "cambio_estado", "vehiculo", idVehiculo,
                "Estado de vehículo '" + vehiculo.get("placa") + "' cambiado a " + nuevoEstado, solicitud);

        log.info("Estado de vehículo alternado: id={}, nuevoEstado={}", idVehiculo, nuevoEstado);

        return Map.of("id", idVehiculo, "estado", nuevoEstado);
    }

    private List<String> procesarYGuardarImagenes(long idVehiculo, List<MultipartFile> archivos, int ordenInicial) {
        if (archivos == null || archivos.isEmpty()) {
            return List.of();
        }

        Path carpetaVehiculo = raizSubidas.resolve("vehicles").resolve(String.valueOf(idVehiculo));
        try {
            Files.createDirectories(carpetaVehiculo);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear la carpeta de imágenes del vehículo", e);
        }

        List<String> almacenadas = new ArrayList<>();
        int orden = ordenInicial + 1;

        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) {
                continue;
            }
            if (orden > 4) {
                break;
            }

            String nombreOriginal = archivo.getOriginalFilename() == null ? "imagen.jpg" : archivo.getOriginalFilename();
            String extension = obtenerExtension(nombreOriginal);
            if (!EXTENSIONES_PERMITIDAS.contains(extension.toLowerCase())) {
                continue;
            }

            String nombreUnico = String.format("vehicle_%d_%s_%d.jpg",
                    idVehiculo, UUID.randomUUID().toString().replace("-", "").substring(0, 10), orden);
            Path destino = carpetaVehiculo.resolve(nombreUnico);

            try {
                byte[] bytesComprimidos = comprimirAJpeg(archivo, 800, 0.75f);
                Files.write(destino, bytesComprimidos);
                long tamanio = bytesComprimidos.length;
                String rutaRelativa = "uploads/vehicles/" + idVehiculo + "/" + nombreUnico;

                vehiculoMutacionDao.insertarImagen(idVehiculo, nombreOriginal, rutaRelativa, tamanio, orden);
                almacenadas.add(rutaRelativa);
                orden++;
            } catch (Exception e) {
                throw new IllegalStateException("Error al procesar y almacenar imagen: " + nombreOriginal, e);
            }
        }

        return almacenadas;
    }

    private byte[] comprimirAJpeg(MultipartFile archivo, int anchoMaximo, float calidad) throws IOException {
        try (InputStream in = archivo.getInputStream()) {
            BufferedImage original = ImageIO.read(in);
            if (original == null) {
                return archivo.getBytes();
            }

            int ancho = original.getWidth();
            int alto = original.getHeight();

            if (ancho > anchoMaximo) {
                alto = (int) (((double) alto / ancho) * anchoMaximo);
                ancho = anchoMaximo;
            }

            BufferedImage rgb = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(original, 0, 0, ancho, alto, Color.WHITE, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(calidad);
                }
                writer.write(null, new IIOImage(rgb, null, null), param);
            } finally {
                writer.dispose();
            }
            return out.toByteArray();
        }
    }

    private String obtenerExtension(String nombre) {
        int indice = nombre.lastIndexOf('.');
        return indice >= 0 ? nombre.substring(indice + 1) : "";
    }

    private void agregarCampoDirecto(String campo, Map<String, Object> entrada, List<String> clausulas, List<Object> parametros) {
        if (entrada.containsKey(campo)) {
            clausulas.add(campo + " = ?");
            parametros.add(textoOpcional(entrada.get(campo)));
        }
    }

    private String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }

    private String textoOpcional(Object valor) {
        String s = texto(valor);
        return s.isBlank() ? null : s;
    }

    private String textoRequerido(String valor, String campo, int min, int max) {
        String limpio = texto(valor);
        if (limpio.length() < min || limpio.length() > max) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Validación fallida: " + campo + " debe tener entre " + min + " y " + max + " caracteres");
        }
        return limpio;
    }

    private Long parsearLongOpcional(Object valor, String mensajeError) {
        if (valor == null || valor.toString().isBlank()) return null;
        try {
            long parsed = Long.parseLong(valor.toString().trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, mensajeError);
        }
    }

    private Integer parsearEnteroOpcional(Object valor) {
        if (valor == null || valor.toString().isBlank()) return null;
        try {
            return Integer.parseInt(valor.toString().trim());
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ano_fabricacion debe ser numérico");
        }
    }

    private LocalDate parsearFechaOpcional(Object valor) {
        if (valor == null || valor.toString().isBlank()) return null;
        try {
            return LocalDate.parse(valor.toString().trim());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Fecha inválida (formato YYYY-MM-DD)");
        }
    }
}
