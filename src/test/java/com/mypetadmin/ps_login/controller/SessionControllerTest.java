package com.mypetadmin.ps_login.controller;

import com.mypetadmin.ps_login.dto.LoginResponse;
import com.mypetadmin.ps_login.dto.RefreshRequest;
import com.mypetadmin.ps_login.service.SessionService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionControllerTest {

    @Test
    void refreshRetornaNovoParDeTokens() {
        SessionService service = mock(SessionService.class);
        SessionController controller = new SessionController(service);
        RefreshRequest request = new RefreshRequest("refresh");
        LoginResponse tokens = new LoginResponse("access", "Bearer", 900, "new-refresh", 2_592_000);
        when(service.refresh("refresh")).thenReturn(tokens);

        var response = controller.refresh(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(tokens);
    }

    @Test
    void logoutEhIdempotenteERetornaNoContent() {
        SessionService service = mock(SessionService.class);
        SessionController controller = new SessionController(service);
        RefreshRequest request = new RefreshRequest("refresh");

        var response = controller.logout(request);

        verify(service).logout("refresh");
        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }
}
