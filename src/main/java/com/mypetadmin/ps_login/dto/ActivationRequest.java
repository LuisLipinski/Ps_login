package com.mypetadmin.ps_login.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivationRequest(
        @NotBlank @Size(max = 256) String token,
        @NotBlank @Size(max = 1024) String password,
        @NotBlank @Size(max = 1024) String passwordConfirmation
) {
}
