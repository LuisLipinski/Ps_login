package com.mypetadmin.ps_login.exception;

public class CredentialAlreadyActiveException extends RuntimeException {
    public CredentialAlreadyActiveException() {
        super("A credencial deste usuário já está ativa.");
    }
}
