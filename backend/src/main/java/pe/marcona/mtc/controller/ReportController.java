package pe.marcona.mtc.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.marcona.mtc.dto.ReportRequest;
import pe.marcona.mtc.service.IReporteService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** API REST para estado del servicio y exportación de reportes a Excel. */
@RestController
@RequestMapping("/api")
public class ReportController {

    private final IReporteService reporteService;

    public ReportController(IReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("servicio", "MTC Marcona Java", "estado", "activo", "fechaHora", LocalDateTime.now().toString());
    }

    /** Recibe registros JSON y devuelve un archivo .xlsx descargable. */
    @PostMapping(value = "/reportes/excel", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> exportarExcel(@RequestBody ReportRequest request) throws IOException {
        try {
            byte[] archivo = reporteService.generarExcel(request);
            String nombreArchivo = "reporte-mtc-marcona-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(archivo.length)
                    .body(archivo);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    private ResponseEntity<byte[]> error(String mensaje) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(mensaje.getBytes(StandardCharsets.UTF_8));
    }
}
