package com.mypetadmin.ps_login.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InvitationRequest(
        @NotNull UUID userId,
        @NotBlank @Email String email,
        UUID requestId
) {
    public InvitationRequest(UUID userId, String email) {
        this(userId, email, null);
    }
}
