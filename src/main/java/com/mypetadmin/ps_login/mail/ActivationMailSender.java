package com.mypetadmin.ps_login.mail;

public interface ActivationMailSender {
    void sendActivation(String email, String rawToken);
}
