package com.mypetadmin.ps_login.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank String password,
        @NotBlank String passwordConfirmation
) {
}
