package com.mypetadmin.ps_login.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Sessão inválida ou expirada.");
    }
}
