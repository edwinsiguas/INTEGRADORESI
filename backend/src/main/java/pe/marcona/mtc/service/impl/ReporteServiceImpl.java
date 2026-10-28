package pe.marcona.mtc.service.impl;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.marcona.mtc.dto.ReportRequest;
import pe.marcona.mtc.service.IReporteService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class ReporteServiceImpl implements IReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteServiceImpl.class);
    private static final int MAX_REGISTROS = 10_000;
    private static final int MAX_COLUMNAS = 16_384;
    private static final int MAX_CARACTERES_CELDA = 32_767;

    @Override
    public byte[] generarExcel(ReportRequest peticion) throws IOException {
        if (peticion == null || peticion.registros() == null || peticion.registros().isEmpty()) {
            throw new IllegalArgumentException("Debe enviar al menos un registro.");
        }
        if (peticion.registros().size() > MAX_REGISTROS) {
            throw new IllegalArgumentException("El límite es " + MAX_REGISTROS + " registros.");
        }
        if (peticion.registros().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Cada registro debe ser un objeto JSON.");
        }

        List<Map<String, Object>> filas = ImmutableList.copyOf(peticion.registros().stream()
                .map(fila -> Collections.unmodifiableMap(new LinkedHashMap<>(fila)))
                .toList());

        String titulo = StringUtils.defaultIfBlank(peticion.titulo(), "Reporte MTC Marcona")
                .replaceAll("[\\[\\]*?/\\\\:]", " ")
                .trim();
        titulo = StringUtils.abbreviate(StringUtils.defaultIfBlank(titulo, "Reporte MTC Marcona"), 80);

        LinkedHashSet<String> conjuntoColumnas = new LinkedHashSet<>();
        filas.forEach(fila -> conjuntoColumnas.addAll(fila.keySet()));
        List<String> columnas = new ArrayList<>(conjuntoColumnas);

        if (columnas.isEmpty()) {
            throw new IllegalArgumentException("Los registros no contienen columnas.");
        }
        if (columnas.size() > MAX_COLUMNAS) {
            throw new IllegalArgumentException("El límite es " + MAX_COLUMNAS + " columnas.");
        }
        if (columnas.stream().anyMatch(columna -> columna.length() > MAX_CARACTERES_CELDA)) {
            throw new IllegalArgumentException("Los nombres de columna no pueden superar " + MAX_CARACTERES_CELDA + " caracteres.");
        }
        for (Map<String, Object> fila : filas) {
            for (String columna : columnas) {
                Object valor = fila.get(columna);
                if (valor != null && String.valueOf(valor).length() > MAX_CARACTERES_CELDA) {
                    throw new IllegalArgumentException("Los valores de celda no pueden superar " + MAX_CARACTERES_CELDA + " caracteres.");
                }
            }
        }

        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet(StringUtils.abbreviate(titulo, 31));
            Font fuenteEncabezado = libro.createFont();
            fuenteEncabezado.setBold(true);
            CellStyle estiloEncabezado = libro.createCellStyle();
            estiloEncabezado.setFont(fuenteEncabezado);
            estiloEncabezado.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row filaTitulo = hoja.createRow(0);
            filaTitulo.createCell(0).setCellValue(titulo);

            Row encabezado = hoja.createRow(2);
            for (int c = 0; c < columnas.size(); c++) {
                Cell celda = encabezado.createCell(c);
                celda.setCellValue(columnas.get(c));
                celda.setCellStyle(estiloEncabezado);
            }

            int indiceFila = 3;
            for (Map<String, Object> registro : filas) {
                Row filaExcel = hoja.createRow(indiceFila++);
                for (int c = 0; c < columnas.size(); c++) {
                    Object valor = registro.get(columnas.get(c));
                    Cell celda = filaExcel.createCell(c);
                    if (valor == null) {
                        celda.setBlank();
                    } else if (valor instanceof Number n) {
                        celda.setCellValue(n.doubleValue());
                    } else if (valor instanceof Boolean b) {
                        celda.setCellValue(b);
                    } else {
                        celda.setCellValue(String.valueOf(valor));
                    }
                }
            }

            for (int c = 0; c < columnas.size(); c++) {
                hoja.autoSizeColumn(c);
            }

            libro.write(salida);
            log.info("Reporte Excel generado exitosamente: {} registros, {} columnas", filas.size(), columnas.size());
            return salida.toByteArray();
        }
    }
}
