package com.mypetadmin.ps_login.exception;

public class CurrentPasswordInvalidException extends RuntimeException {
    public CurrentPasswordInvalidException() {
        super("Senha atual inválida.");
    }
}
