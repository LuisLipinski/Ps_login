package com.mypetadmin.ps_login.mail;

public interface PasswordResetMailSender {
    void sendReset(String email, String rawToken);
}
