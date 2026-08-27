package com.mypetadmin.ps_login.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpPasswordResetMailSender implements PasswordResetMailSender {

    private final JavaMailSender javaMailSender;
    private final String from;
    private final String resetUrl;

    public SmtpPasswordResetMailSender(
            JavaMailSender javaMailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.password-reset.url}") String resetUrl) {
        this.javaMailSender = javaMailSender;
        this.from = from;
        this.resetUrl = resetUrl;
    }

    @Override
    public void sendReset(String email, String rawToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Redefinição de senha — My Pet Admin");
        message.setText("Recebemos uma solicitação para redefinir sua senha. Acesse o link abaixo:\n\n"
                + resetUrl + "?token=" + rawToken
                + "\n\nSe você não solicitou esta alteração, ignore este e-mail.");
        javaMailSender.send(message);
    }
}
