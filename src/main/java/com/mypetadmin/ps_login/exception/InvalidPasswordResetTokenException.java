package com.mypetadmin.ps_login.exception;

public class InvalidPasswordResetTokenException extends RuntimeException {
    public InvalidPasswordResetTokenException() {
        super("Token de redefinição inválido ou expirado.");
    }
}
