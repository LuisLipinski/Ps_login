package com.mypetadmin.ps_login.controller;

import com.mypetadmin.ps_login.dto.ChangePasswordRequest;
import com.mypetadmin.ps_login.dto.ForgotPasswordRequest;
import com.mypetadmin.ps_login.dto.ResetPasswordRequest;
import com.mypetadmin.ps_login.exception.InvalidCredentialsException;
import com.mypetadmin.ps_login.service.PasswordService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auth/password")
public class PasswordController {

    private final PasswordService passwordService;

    public PasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @PostMapping("/forgot")
    public ResponseEntity<Void> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordService.requestReset(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
        passwordService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change")
    public ResponseEntity<Void> change(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request) {
        passwordService.changePassword(userId(jwt), request);
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new IllegalArgumentException();
            }
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new InvalidCredentialsException();
        }
    }
}
