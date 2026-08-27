package com.mypetadmin.ps_login.controller;

import com.mypetadmin.ps_login.config.JwtConfig;
import com.mypetadmin.ps_login.config.SecurityConfig;
import com.mypetadmin.ps_login.exception.GlobalExceptionHandler;
import com.mypetadmin.ps_login.security.InternalRequestFilter;
import com.mypetadmin.ps_login.service.PasswordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PasswordController.class)
@Import({SecurityConfig.class, JwtConfig.class, InternalRequestFilter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "security.internal-key=test-key",
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class PasswordControllerSecurityTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PasswordService passwordService;

    @Test
    void forgotEhPublicoERetornaRespostaNeutra() throws Exception {
        mockMvc.perform(post("/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isAccepted());
        verify(passwordService).requestReset(any());
    }

    @Test
    void resetEhPublico() throws Exception {
        mockMvc.perform(post("/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token\",\"password\":\"nova-senha-segura\",\"passwordConfirmation\":\"nova-senha-segura\"}"))
                .andExpect(status().isNoContent());
        verify(passwordService).resetPassword(any());
    }

    @Test
    void changeExigeBearerTokenValido() throws Exception {
        mockMvc.perform(post("/auth/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"atual\",\"newPassword\":\"nova-senha-segura\",\"newPasswordConfirmation\":\"nova-senha-segura\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changeUsaSubjectDoJwtComoUserId() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/auth/password/change")
                        .with(jwt().jwt(token -> token.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"atual\",\"newPassword\":\"nova-senha-segura\",\"newPasswordConfirmation\":\"nova-senha-segura\"}"))
                .andExpect(status().isNoContent());
        verify(passwordService).changePassword(org.mockito.ArgumentMatchers.eq(userId), any());
    }

    @Test
    void changeRejeitaSubjectQueNaoEhUuid() throws Exception {
        mockMvc.perform(post("/auth/password/change")
                        .with(jwt().jwt(token -> token.subject("subject-invalido")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"atual\",\"newPassword\":\"nova-senha-segura\",\"newPasswordConfirmation\":\"nova-senha-segura\"}"))
                .andExpect(status().isUnauthorized());
    }
}
