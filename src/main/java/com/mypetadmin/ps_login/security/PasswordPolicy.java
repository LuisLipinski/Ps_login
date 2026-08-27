package com.mypetadmin.ps_login.security;

import com.mypetadmin.ps_login.exception.PasswordPolicyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PasswordPolicy {

    private static final int MAX_LENGTH = 128;
    private final int minLength;

    public PasswordPolicy(@Value("${security.password.min-length:12}") int minLength) {
        this.minLength = minLength;
    }

    public void validate(String password, String confirmation) {
        if (!Objects.equals(password, confirmation)) {
            throw new PasswordPolicyException("As senhas informadas não conferem.");
        }
        if (password.length() < minLength) {
            throw new PasswordPolicyException("A senha não atende ao tamanho mínimo configurado.");
        }
        if (password.length() > MAX_LENGTH) {
            throw new PasswordPolicyException("A senha excede o tamanho máximo permitido.");
        }
    }
}
