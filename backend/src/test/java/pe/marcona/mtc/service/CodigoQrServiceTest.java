package pe.marcona.mtc.service;

import org.junit.jupiter.api.Test;
import pe.marcona.mtc.service.impl.CodigoQrServiceImpl;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class CodigoQrServiceTest {
    private final ICodigoQrService codigoQrService = new CodigoQrServiceImpl();

    @Test
    void generatesValidBase64PngFromText() {
        String base64 = codigoQrService.generarQrBase64("https://transporte.munimarcona.gob.pe/tuc/PERM-2026-00001");
        assertThat(base64).isNotBlank();

        byte[] decoded = Base64.getDecoder().decode(base64);
        assertThat(decoded).isNotEmpty();
        // Check PNG magic bytes: 0x89 0x50 0x4E 0x47
        assertThat(decoded[0]).isEqualTo((byte) 0x89);
        assertThat(decoded[1]).isEqualTo((byte) 0x50);
        assertThat(decoded[2]).isEqualTo((byte) 0x4E);
        assertThat(decoded[3]).isEqualTo((byte) 0x47);
    }

    @Test
    void returnsNullForBlankInput() {
        assertThat(codigoQrService.generarQrBase64("")).isNull();
        assertThat(codigoQrService.generarQrBase64(null)).isNull();
    }
}
