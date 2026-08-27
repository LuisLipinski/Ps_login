package com.mypetadmin.ps_login.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SmtpActivationMailSender implements ActivationMailSender {

    private final JavaMailSender mailSender;
    private final String from;
    private final String activationUrl;

    public SmtpActivationMailSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.activation.url}") String activationUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.activationUrl = activationUrl;
    }

    @Override
    public void sendActivation(String email, String rawToken) {
        String link = UriComponentsBuilder.fromUriString(activationUrl)
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Ative seu acesso ao My Pet Admin");
        message.setText("Seu acesso ao My Pet Admin foi criado. Defina sua senha pelo link: " + link);
        mailSender.send(message);
    }
}
