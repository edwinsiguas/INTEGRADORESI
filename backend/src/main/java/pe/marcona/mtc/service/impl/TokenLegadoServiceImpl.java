package pe.marcona.mtc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.marcona.mtc.service.ITokenLegadoService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementación de generación y validación criptográfica de tokens firmados.
 */
@Service
public class TokenLegadoServiceImpl implements ITokenLegadoService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Base64.Encoder CODIFICADOR = Base64.getEncoder();
    private static final Base64.Decoder DECODIFICADOR = Base64.getDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secreto;
    private final long segundosExpiracion;

    public TokenLegadoServiceImpl(
            ObjectMapper objectMapper,
            @Value("${app.-secret}") String secret,
            @Value("${app.-expiry-seconds:86400}") long expirySeconds,
            @Value("${app.environment:development}") String environment) {
        if (secret.isBlank() || ("dev_only_change_me".equals(secret) && "production".equalsIgnoreCase(environment))) {
            throw new IllegalStateException("Configure _SECRET antes de iniciar Java en producción.");
        }
        if (expirySeconds < 1) {
            throw new IllegalArgumentException("_EXPIRY debe ser mayor que cero.");
        }
        this.objectMapper = objectMapper;
        this.secreto = secret.getBytes(StandardCharsets.UTF_8);
        this.segundosExpiracion = expirySeconds;
    }

    @Override
    public String generar(long idUsuario, String rol) {
        try {
            String cabecera = codificar(Map.of("alg", "HS256", "typ", ""));
            var reclamos = new LinkedHashMap<String, Object>();
            reclamos.put("user_id", idUsuario);
            reclamos.put("role", rol);
            reclamos.put("iat", Instant.now().getEpochSecond());
            reclamos.put("exp", Instant.now().getEpochSecond() + segundosExpiracion);
            String cuerpo = codificar(reclamos);
            String contenido = cabecera + "." + cuerpo;
            return contenido + "." + CODIFICADOR.encodeToString(firmar(contenido));
        } catch (JsonProcessingException | GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo generar el token de sesión.", ex);
        }
    }

    @Override
    public Map<String, Object> verificar(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String[] partes = token.split("\\.", -1);
        if (partes.length != 3) {
            return null;
        }

        try {
            byte[] firmaRecibida = DECODIFICADOR.decode(partes[2]);
            byte[] firmaEsperada = firmar(partes[0] + "." + partes[1]);
            if (!MessageDigest.isEqual(firmaEsperada, firmaRecibida)) {
                return null;
            }

            var reclamos = objectMapper.readValue(DECODIFICADOR.decode(partes[1]), new TypeReference<Map<String, Object>>() {});
            Object expiracion = reclamos.get("exp");
            Object idUsuario = reclamos.get("user_id");
            if (!(expiracion instanceof Number exp)
                    || exp.longValue() < Instant.now().getEpochSecond()
                    || (!(idUsuario instanceof Number)
                    && !(idUsuario instanceof String id && id.matches("\\d+")))) {
                return null;
            }
            return reclamos;
        } catch (IllegalArgumentException | IOException | GeneralSecurityException ex) {
            return null;
        }
    }

    private String codificar(Object valor) throws JsonProcessingException {
        return CODIFICADOR.encodeToString(objectMapper.writeValueAsBytes(valor));
    }

    private byte[] firmar(String contenido) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(secreto, HMAC_SHA256));
        return mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
    }
}

