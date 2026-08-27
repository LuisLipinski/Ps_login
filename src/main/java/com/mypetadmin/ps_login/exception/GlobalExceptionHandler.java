package com.mypetadmin.ps_login.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ErrorResponse> invalidCredentials(InvalidCredentialsException ex) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<ErrorResponse> invalidRefresh(InvalidRefreshTokenException ex) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_SESSION", ex.getMessage());
    }

    @ExceptionHandler(ActivationTokenInvalidException.class)
    ResponseEntity<ErrorResponse> invalidToken(ActivationTokenInvalidException ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_ACTIVATION_TOKEN", ex.getMessage());
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    ResponseEntity<ErrorResponse> invalidPasswordResetToken(InvalidPasswordResetTokenException ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD_RESET_TOKEN", ex.getMessage());
    }

    @ExceptionHandler(CurrentPasswordInvalidException.class)
    ResponseEntity<ErrorResponse> currentPasswordInvalid(CurrentPasswordInvalidException ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", ex.getMessage());
    }

    @ExceptionHandler(PasswordPolicyException.class)
    ResponseEntity<ErrorResponse> passwordPolicy(PasswordPolicyException ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", ex.getMessage());
    }

    @ExceptionHandler({CredentialAlreadyActiveException.class, IdentityValidationException.class})
    ResponseEntity<ErrorResponse> conflict(RuntimeException ex) {
        return response(HttpStatus.CONFLICT, "ACTIVATION_CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(PsUserIntegrationException.class)
    ResponseEntity<ErrorResponse> integration(PsUserIntegrationException ex) {
        return response(HttpStatus.BAD_GATEWAY, "PS_USER_INTEGRATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dados da requisição inválidos.");
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(), code, message));
    }
}
