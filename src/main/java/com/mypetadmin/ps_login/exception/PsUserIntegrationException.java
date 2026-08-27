package com.mypetadmin.ps_login.exception;

public class PsUserIntegrationException extends RuntimeException {
    public PsUserIntegrationException() {
        super("Não foi possível validar a identidade do usuário.");
    }
}
