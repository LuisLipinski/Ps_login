package com.mypetadmin.ps_login.controller;

import com.mypetadmin.ps_login.dto.LoginRequest;
import com.mypetadmin.ps_login.dto.LoginResponse;
import com.mypetadmin.ps_login.service.LoginService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginControllerTest {

    @Test
    void retornaTokensQuandoAutenticacaoEhValida() {
        LoginService service = mock(LoginService.class);
        LoginController controller = new LoginController(service);
        LoginRequest request = new LoginRequest("user@example.com", "senha");
        LoginResponse tokens = new LoginResponse("access", "Bearer", 900, "refresh", 2_592_000);
        when(service.login(request)).thenReturn(tokens);

        var response = controller.login(request);

        verify(service).login(request);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(tokens);
    }
}
