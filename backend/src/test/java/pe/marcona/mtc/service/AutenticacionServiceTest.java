package pe.marcona.mtc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pe.marcona.mtc.dao.IAutenticacionDao;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.impl.AutenticacionServiceImpl;
import pe.marcona.mtc.service.impl.TokenLegadoServiceImpl;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AutenticacionServiceTest {
    private final IAutenticacionDao autenticacionDao = mock(IAutenticacionDao.class);
    private final ILimitePeticionesService limitePeticionesService = mock(ILimitePeticionesService.class);
    private final IAuditoriaService auditoriaService = mock(IAuditoriaService.class);
    private final ITokenLegadoService tokenLegadoService =
            new TokenLegadoServiceImpl(new ObjectMapper(), "test-secret", 3600, "test");
    private final IAutenticacionService autenticacionService =
            new AutenticacionServiceImpl(autenticacionDao, tokenLegadoService, limitePeticionesService, auditoriaService);

    @Test
    void loginAcceptsExistingPhpBcryptHashesAndIssuesACompatibleToken() {
        String passwordHash = new BCryptPasswordEncoder(4).encode("valid-password")
                .replaceFirst("^\\$2a\\$", "\\$2y\\$");
        when(autenticacionDao.obtenerUsuarioActivoPorEmail("user@example.com"))
                .thenReturn(Optional.of(Map.of(
                        "id", 17L,
                        "email", "user@example.com",
                        "password", passwordHash,
                        "nombre", "Usuario",
                        "role", "admin",
                        "estado", "activo",
                        "is_active", true)));
        MockHttpServletRequest request = request();

        Map<String, Object> response = autenticacionService.iniciarSesion(
                Map.of("email", "user@example.com", "password", "valid-password"), request);

        assertThat(tokenLegadoService.verificar((String) response.get("token")))
                .containsEntry("user_id", 17)
                .containsEntry("role", "admin");
        assertThat(response).doesNotContainKey("password");
        verify(limitePeticionesService).verificar("login", "127.0.0.1", 5, 900);
        verify(auditoriaService).registrarAccion(17, "login", "usuario", 17, "Login exitoso", request);
    }

    @Test
    void rejectsAnInactiveUserEvenWhenTheTokenSignatureIsValid() {
        String token = tokenLegadoService.generar(17, "admin");
        when(autenticacionDao.obtenerUsuarioPorId(17L))
                .thenReturn(Optional.of(Map.of(
                        "id", 17L,
                        "email", "user@example.com",
                        "nombre", "Usuario",
                        "role", "admin",
                        "estado", "inactivo",
                        "is_active", true)));
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Bearer " + token);

        assertThatThrownBy(() -> autenticacionService.requerirUsuario(request, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Usuario inválido o inactivo");
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
