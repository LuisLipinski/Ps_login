package com.mypetadmin.ps_login.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 1024) String currentPassword,
        @NotBlank @Size(max = 1024) String newPassword,
        @NotBlank @Size(max = 1024) String newPasswordConfirmation
) {
}
