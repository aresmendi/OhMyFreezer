package com.ares.backend.controller;

import com.ares.backend.config.JwtUtil;
import com.ares.backend.dto.LoginResponse;
import com.ares.backend.dto.UsuarioLoginRequest;
import com.ares.backend.dto.UsuarioResponse;
import com.ares.backend.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para UsuarioController, en particular login(): el token
 * emitido debe llevar el negocioId del usuario autenticado (multi-tenancy),
 * nunca uno propuesto por el cliente.
 */
class UsuarioControllerTest {

    @Test
    @DisplayName("login() genera el token con el negocioId del usuario autenticado")
    void login_generaTokenConNegocioIdDelUsuario() {
        UsuarioService usuarioService = mock(UsuarioService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        UsuarioController controller = new UsuarioController(usuarioService, jwtUtil);

        UsuarioResponse usuarioResponse =
                new UsuarioResponse(9L, "jefe1", true, null, "jefe1@test.com", 3L);
        UsuarioLoginRequest request = new UsuarioLoginRequest();
        request.setUsername("jefe1");
        request.setPassword("password123");

        when(usuarioService.login(request)).thenReturn(usuarioResponse);
        when(jwtUtil.generarToken(9L, "jefe1", true, 3L)).thenReturn("token-negocio-3");

        ResponseEntity<LoginResponse> response = controller.login(request);

        assertThat(response.getBody().getToken()).isEqualTo("token-negocio-3");
        verify(jwtUtil).generarToken(9L, "jefe1", true, 3L);
    }

    @Test
    @DisplayName("login() con otro negocio genera el token con ESE negocioId (triangulación)")
    void login_conOtroNegocio_generaTokenConEseNegocioId() {
        UsuarioService usuarioService = mock(UsuarioService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        UsuarioController controller = new UsuarioController(usuarioService, jwtUtil);

        UsuarioResponse usuarioResponse =
                new UsuarioResponse(10L, "jefe2", true, null, "jefe2@test.com", 55L);
        UsuarioLoginRequest request = new UsuarioLoginRequest();
        request.setUsername("jefe2");
        request.setPassword("password123");

        when(usuarioService.login(request)).thenReturn(usuarioResponse);
        when(jwtUtil.generarToken(10L, "jefe2", true, 55L)).thenReturn("token-negocio-55");

        ResponseEntity<LoginResponse> response = controller.login(request);

        assertThat(response.getBody().getToken()).isEqualTo("token-negocio-55");
        verify(jwtUtil).generarToken(10L, "jefe2", true, 55L);
    }
}
