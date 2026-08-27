package com.mypetadmin.ps_login.controller;

import com.mypetadmin.ps_login.dto.ActivationRequest;
import com.mypetadmin.ps_login.service.ActivationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ActivationControllerTest {

    @Test
    void retornaNoContentAposAtivacao() {
        ActivationService service = mock(ActivationService.class);
        ActivationController controller = new ActivationController(service);
        ActivationRequest request = new ActivationRequest("token", "SenhaSegura123", "SenhaSegura123");

        var response = controller.activate(request);

        verify(service).activate(request);
        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }
}
