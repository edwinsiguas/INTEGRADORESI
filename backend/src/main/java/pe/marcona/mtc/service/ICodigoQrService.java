package pe.marcona.mtc.service;

/**
 * Contrato de servicio para la generación de códigos QR bidimensionales codificados en Base64.
 */
public interface ICodigoQrService {

    String generarQrBase64(String texto, int ancho, int alto);

    String generarQrBase64(String texto);
}
