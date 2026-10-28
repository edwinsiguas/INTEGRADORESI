package pe.marcona.mtc.service;

import org.junit.jupiter.api.Test;
import pe.marcona.mtc.dao.ICirculacionDao;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.impl.CirculacionServiceImpl;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CirculacionServiceTest {
    private final ICirculacionDao circulacionDao = mock(ICirculacionDao.class);
    private final ICirculacionService service = new CirculacionServiceImpl(circulacionDao);

    @Test
    void validatesDniFormatBeforeQueryingTheDatabase() {
        assertThatThrownBy(() -> service.validar(Map.of("dni", "123")))
                .isInstanceOf(ApiException.class)
                .hasMessage("dni debe tener 8 dígitos");
    }

    @Test
    void returnsTheExistingNotFoundResponseForAnUnassignedPlate() {
        when(circulacionDao.buscarPorPlaca("ABC-123")).thenReturn(Optional.empty());

        Map<String, Object> result = service.validar(Map.of("placa", "abc-123"));

        assertThat(result)
                .containsEntry("puede_circular", false)
                .containsEntry("verificado", false)
                .containsEntry("razon", "Vehículo no encontrado o sin conductor asignado actualmente")
                .containsEntry("data", null);
    }
}

