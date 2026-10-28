package pe.marcona.mtc.service;

import java.util.Map;

/**
 * Contrato de servicio para la generación y verificación de tokens de sesión firmados (HMAC-SHA256).
 */
public interface ITokenLegadoService {

    String generar(long idUsuario, String rol);

    Map<String, Object> verificar(String token);
}
