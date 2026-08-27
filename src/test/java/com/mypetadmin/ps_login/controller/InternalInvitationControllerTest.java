package com.mypetadmin.ps_login.controller;

import com.mypetadmin.ps_login.dto.InvitationRequest;
import com.mypetadmin.ps_login.service.InvitationService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InternalInvitationControllerTest {

    @Test
    void retornaAcceptedAposCriarConvite() {
        InvitationService service = mock(InvitationService.class);
        InternalInvitationController controller = new InternalInvitationController(service);
        InvitationRequest request = new InvitationRequest(UUID.randomUUID(), "user@example.com");

        var response = controller.createInvitation(request);

        verify(service).createInvitation(request);
        assertThat(response.getStatusCode().value()).isEqualTo(202);
    }
}
