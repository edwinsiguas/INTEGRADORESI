package pe.marcona.mtc.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.IAutenticacionService;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IAutenticacionService autenticacionService;

    @Test
    void loginKeepsTheExistingResponseEnvelope() throws Exception {
        when(autenticacionService.iniciarSesion(any(), any())).thenReturn(Map.of(
                "id", 8,
                "email", "admin@example.com",
                "nombre", "Administradora",
                "role", "admin",
                "token", "issued-token"));

        mockMvc.perform(post("/api/auth/login.php")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@example.com","password":"example-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.token").value("issued-token"))
                .andExpect(jsonPath("$.message").value("Login exitoso"));
    }

    @Test
    void logoutRequiresTheExistingSessionAndReturnsNullData() throws Exception {
        when(autenticacionService.cerrarSesion(any())).thenReturn(null);

        mockMvc.perform(post("/api/auth/logout.php").header("X-Auth-Token", "legacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("Logout exitoso"));
    }

    @Test
    void invalidLoginRetainsTheApiErrorShape() throws Exception {
        when(autenticacionService.iniciarSesion(any(), any()))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED,
                        "Email o contraseña incorrectos"));

        mockMvc.perform(post("/api/auth/login.php")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("Email o contraseña incorrectos"));
    }
}
