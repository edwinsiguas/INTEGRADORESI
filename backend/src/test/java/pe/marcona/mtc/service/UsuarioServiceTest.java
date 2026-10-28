package pe.marcona.mtc.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import pe.marcona.mtc.dao.IUsuarioDao;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.impl.UsuarioServiceImpl;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {
    private final IUsuarioDao usuarioDao = mock(IUsuarioDao.class);
    private final AuthService authService = mock(AuthService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final UsuarioServiceImpl usuarioService = new UsuarioServiceImpl(usuarioDao, authService, auditService);
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void preventsNonAdminUsersFromEditingAnotherAccount() {
        when(authService.requireUser(eq(request), eq(null)))
                .thenReturn(Map.of("id", 4, "role", "operador"));

        assertThatThrownBy(() -> usuarioService.actualizar(
                        Map.of("id", 9, "nombre", "Nombre nuevo"), request))
                .isInstanceOf(ApiException.class)
                .hasMessage("No tienes permiso para editar este usuario");

        verify(usuarioDao, never()).actualizarCampos(any(), any());
    }
}
