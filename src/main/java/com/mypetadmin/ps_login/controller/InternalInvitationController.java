package com.mypetadmin.ps_login.controller;

import com.mypetadmin.ps_login.dto.InvitationRequest;
import com.mypetadmin.ps_login.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth/invitations")
public class InternalInvitationController {

    private final InvitationService invitationService;

    public InternalInvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping
    public ResponseEntity<Void> createInvitation(@Valid @RequestBody InvitationRequest request) {
        invitationService.createInvitation(request);
        return ResponseEntity.accepted().build();
    }
}
