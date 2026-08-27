package com.mypetadmin.ps_login.exception;

public class ActivationTokenInvalidException extends RuntimeException {
    public ActivationTokenInvalidException() {
        super("Token de ativação inválido ou expirado.");
    }
}
