package com.mypetadmin.ps_login.controller;

import com.mypetadmin.ps_login.config.JwtConfig;
import com.mypetadmin.ps_login.config.SecurityConfig;
import com.mypetadmin.ps_login.exception.GlobalExceptionHandler;
import com.mypetadmin.ps_login.exception.InvalidCredentialsException;
import com.mypetadmin.ps_login.security.InternalRequestFilter;
import com.mypetadmin.ps_login.service.LoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoginController.class)
@Import({SecurityConfig.class, JwtConfig.class, InternalRequestFilter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "security.internal-key=test-key",
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class LoginControllerSecurityTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean LoginService loginService;

    @Test
    void credenciaisInvalidasRetornamRespostaGenericaSemDetalharConta() throws Exception {
        when(loginService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"SenhaInvalida123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas."))
                .andExpect(content().string(not(containsString("user@example.com"))))
                .andExpect(content().string(not(containsString("inativo"))))
                .andExpect(content().string(not(containsString("não encontrado"))));
    }

    @Test
    void payloadInvalidoNaoEhConfundidoComFalhaDeCredencial() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"email-invalido\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejeitaEmailESenhaAcimaDosLimitesDoContrato() throws Exception {
        String emailMuitoLongo = "a".repeat(245) + "@example.com";
        String senhaMuitoLonga = "x".repeat(129);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + emailMuitoLongo + "\",\"password\":\"" + senhaMuitoLonga + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void jsonMalformadoRetornaErroControlado() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.message").value("JSON da requisição inválido."));
    }

    @Test
    void contentTypeNaoSuportadoRetorna415Controlado() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"email\":\"user@example.com\",\"password\":\"SenhaInvalida123!\"}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }
}
