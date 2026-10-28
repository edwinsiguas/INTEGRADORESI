package pe.marcona.mtc.service;

import pe.marcona.mtc.dto.ReportRequest;

import java.io.IOException;

/**
 * Contrato de servicio para la generación y maquetación de reportes tabulares en formato Excel (.xlsx).
 */
public interface IReporteService {

    byte[] generarExcel(ReportRequest peticion) throws IOException;
}
